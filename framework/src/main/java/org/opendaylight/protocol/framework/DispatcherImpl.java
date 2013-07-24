/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public final class DispatcherImpl implements Dispatcher, SessionParent {

	private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

	public static final int DEFAULT_MAX_RECONNECT_COUNT = 30;

	public static final int DEFAULT_RECONNECT_MILLIS = 30000;
	public static final int DEFAULT_SERVICE_MILLIS = 1000;

	private static final int BUFFER_SIZE = 16384;

	private static final boolean SSL_ENABLED = true;

	private int serviceMillis = DEFAULT_SERVICE_MILLIS;
	private int reconnectMillis = 5000;

	private int maxConnectCount = 0;

	/**
	 * List of servers created by this dispatcher. Servers are identified as a pair Server and the InetSocketAddress to
	 * which the server is bound.
	 */
	private final Map<InetSocketAddress, ProtocolServer> servers = new HashMap<InetSocketAddress, ProtocolServer>();

	/**
	 * Mapping of client Sessions to keys (Either clients created by the dispatcher directly or clients connected to one
	 * of the dispatchers server).
	 */
	private final Map<ProtocolSession, SelectionKey> sessionKeys = new HashMap<ProtocolSession, SelectionKey>();

	/**
	 * List of clients created by this dispatcher. Each client has its own Session. They are identified as a pair of
	 * Session and the InetSocketAddress to which they are connected.
	 */
	private final BiMap<InetSocketAddress, ProtocolSession> clients;

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	/**
	 * Variable indicating that there was a request for stopping this dispatcher.
	 */
	private volatile boolean requestStop = false;

	private final Thread innerThread;

	private final ExecutorService executorService;

	private final InnerRun innerRun;

	/**
	 * Configuration dependency used for testing of reusability.
	 */
	private final ThreadFactory threadFactory;

	private final class InnerRun implements Runnable {

		/**
		 * Common selector for client/server parts.
		 */
		public final Selector selector;

		private final DispatcherImpl parent;

		protected InnerRun(final DispatcherImpl parent) throws IOException {
			final Selector s = SelectorProvider.provider().openSelector();

			if (SSL_ENABLED)
				this.selector = new SSLSelector(s);
			else
				this.selector = s;

			this.parent = parent;
		}

		@Override
		public void run() {
			// this method finishes only when stop() method was called
			while (!this.parent.requestStop) {
				try {
					this.selector.select();
				} catch (final IOException e) {
					logger.warn("Selection operation failed", e);
					break;
				}

				/*
				 * This block runs under lock. The idea is that
				 * selection key notifiers will first acquire the
				 * lock, then wake up the selector, then do their
				 * modifications.
				 *
				 * This means that there are two possibilities:
				 *
				 * 1) we arrive here as a result of a selector wake
				 *    up, at which point the modifier already holds
				 *    the lock, and we'll wait for it.
				 *
				 * 2) we arrive here as a result of an event, in which
				 *    case we will prevent modifiers from starting
				 *    by holding the lock.
				 */
				// logger.debug("Acquiring lock");
				synchronized (this) {
					final Set<SelectionKey> keys = this.selector.selectedKeys();
					if (keys.isEmpty())
						continue;

					/*
					 * Calculate maximum nanoseconds we can spend on read
					 * or write. Each key can do a pair of operations in one
					 * iteration.
					 */
					final long serviceTime = serviceMillis * 500000 / keys.size();

					final Iterator<SelectionKey> selectedKeys = keys.iterator();
					while (selectedKeys.hasNext()) {
						final SelectionKey key = selectedKeys.next();
						selectedKeys.remove();

						if (!key.isValid()) {
							continue;
						}

						try {
							if (key.isAcceptable()) {
								this.parent.accept(key);
							}
							if (key.isConnectable()) {
								if (!this.parent.finishConnection(key)) {
									continue;
								}
							}

							/*
							 * Split read/write fairness. If this key is only
							 * readable or only writable, double the time
							 */
							final long keyTime = key.isReadable() == key.isWritable() ? serviceTime : 2 * serviceTime;

							/*
							 * If this key is readable, read it. That operation may
							 * detect end-of-stream, which it will report internally,
							 * and inform us by returning true.
							 *
							 * If that is the case, we do not want to proceed with the
							 * write case, because the key may no longer be valid.
							 */
							if (key.isReadable() && this.parent.read(key, System.nanoTime() + keyTime)) {
								continue;
							}

							/*
							 * If this key is writable, write it. This may completely
							 * drain the output queue, in which case write returns true.
							 *
							 * If that is the case, we need to suspend selecting for
							 * writability -- it will be re-enabled once the queue goes
							 * non-empty.
							 */
							if (key.isWritable() && this.parent.write(key, System.nanoTime() + keyTime) && key.isValid()) {
								key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
							}

						} catch (final IOException e) {
							logger.debug("Channel {} incurred unexpected error, closing it", key.channel(), e);
							key.cancel();
							try {
								key.channel().close(); // close the channel that caused problems
							} catch (final IOException e1) {
								logger.error("Channel: {} could not be closed, because {}", key.channel(), e1.getMessage(), e1);
							}
						}
					}
				}
			}

			logger.trace("Ended run of dispatcher.");
			try {
				this.selector.close();
			} catch (final IOException e) {
				throw new RuntimeException("Failed to close selector", e);
			}
		}
	}

	/**
	 * Creates an instance of Dispatcher, gets the default selector and opens it.
	 *
	 * @param tfactory default Thread Factory
	 * @throws IOException if some error occurred during opening the selector
	 */
	public DispatcherImpl(final ThreadFactory tfactory) throws IOException {
		this.threadFactory = tfactory;
		this.executorService = Executors.newSingleThreadExecutor(tfactory);
		this.stateTimer = new Timer();
		this.clients = HashBiMap.create();
		this.innerRun = new InnerRun(this);
		this.innerThread = tfactory.newThread(this.innerRun);
		this.innerThread.start();
	}

	protected synchronized ProtocolServer startServer(final ServerSocketChannel serverChannel, final InetSocketAddress address,
			final ProtocolConnectionFactory connectionFactory, final ProtocolSessionFactory sfactory,
			final ProtocolInputStreamFactory isFactory) throws IOException {

		// Notify the thread to update its selection keys
		this.innerRun.selector.wakeup();

		// logger.debug("Selector notified.");
		serverChannel.configureBlocking(false);
		serverChannel.bind(address);

		final SelectionKey key = serverChannel.register(this.innerRun.selector, SelectionKey.OP_ACCEPT);
		final ProtocolServer server = new ProtocolServer(this, address, serverChannel, connectionFactory, sfactory, isFactory);
		key.attach(server);
		this.servers.put(address, server);

		logger.info("Server created.");
		return server;
	}

	@Override
	public ProtocolServer createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sfactory, final ProtocolInputStreamFactory isFactory) throws IOException {
		synchronized (this.innerRun) {
			if (this.servers.get(address) != null) {
				logger.warn("Server with this address: {} was already created.", address);
				throw new IllegalStateException("Server with this address: " + address + " was already created.");
			}

			return this.startServer(ServerSocketChannel.open(), address, connectionFactory, sfactory, isFactory);
		}
	}

	@Override
	public ProtocolServer createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sfactory, final ProtocolInputStreamFactory isFactory, final SSLContext context) throws IOException {

		if (!SSL_ENABLED)
			throw new UnsupportedOperationException("SSL has not been enabled");

		synchronized (this.innerRun) {
			if (this.servers.get(address) != null) {
				logger.warn("Server with this address: {} was already created.", address);
				throw new IllegalStateException("Server with this address: " + address + " was already created.");
			}

			return this.startServer(SSLServerSocketChannel.open(this.innerRun.selector, context, this.executorService), address,
					connectionFactory, sfactory, isFactory);
		}
	}

	private void connectChannel(final SelectionKey key) {
		final SessionStreams state = (SessionStreams) key.attachment();
		state.timer = null;

		state.connectCount++;
		logger.debug("Connecting to {} attempt {}", state.connection.getPeerAddress(), state.connectCount);

		final SocketChannel channel = (SocketChannel) key.channel();
		try {
			channel.connect(state.connection.getPeerAddress());
		} catch (final IOException e) {
			this.connectFailed(key, e);
			return;
		}

		if (channel.isConnected()) {
			logger.trace("Connected, update interestops");
			key.interestOps(SelectionKey.OP_READ);
			state.getSession().startSession();
		} else
			key.interestOps(SelectionKey.OP_CONNECT);
	}

	private void connectFailed(final SelectionKey key, final IOException e) {
		final SessionStreams state = (SessionStreams) key.attachment();

		key.interestOps(0);

		if (this.maxConnectCount >= 0 && state.connectCount >= this.maxConnectCount) {
			logger.debug("Connection to {} failed", state.connection.getPeerAddress().getAddress(), e);
			this.clients.inverse().remove(state.getSession());
			state.getSession().onConnectionFailed(e);
			return;
		}

		logger.trace("Connect to {} failed, will retry in {} milliseconds", state.connection.getPeerAddress().getAddress(),
				this.reconnectMillis, e);
		state.timer = new TimerTask() {
			@Override
			public void run() {
				DispatcherImpl.this.connectChannel(key);
			}
		};
		this.stateTimer.schedule(state.timer, this.reconnectMillis);
	}

	private ProtocolSession startClient(final SocketChannel channel, final ProtocolConnection connection,
			final ProtocolSessionFactory sfactory, final ProtocolInputStreamFactory isFactory) throws IOException {

		// Notify the thread to update its selection keys
		this.innerRun.selector.wakeup();

		channel.configureBlocking(false);

		final ProtocolSession session;
		final SelectionKey key;
		synchronized (this) {
			session = sfactory.getProtocolSession(this, this.stateTimer, connection, 0);

			final PipedOutputStream pos = new PipedOutputStream();
			final PipedInputStream pis = new PipedInputStream(pos, session.maximumMessageSize());

			key = channel.register(this.innerRun.selector, SelectionKey.OP_CONNECT);
			key.attach(new SessionStreams(pos, pis, isFactory.getProtocolInputStream(pis, session.getMessageFactory()), session, connection));

			this.sessionKeys.put(session, key);
			this.clients.put(connection.getPeerAddress(), session);
			logger.info("Client created.");
		}

		this.connectChannel(key);
		return session;
	}

	@Override
	public ProtocolSession createClient(final ProtocolConnection connection, final ProtocolSessionFactory sfactory,
			final ProtocolInputStreamFactory isFactory) throws IOException {
		synchronized (this.innerRun) {
			if (this.clients.containsKey(connection.getPeerAddress())) {
				logger.warn("Attempt to create duplicate client session to the same address: {}", connection.getPeerAddress());
				throw new IllegalStateException("Attempt to create duplicate client session to the same address: "
						+ connection.getPeerAddress());
			}

			return this.startClient(SocketChannel.open(), connection, sfactory, isFactory);
		}
	}

	@Override
	public ProtocolSession createClient(final ProtocolConnection connection, final ProtocolSessionFactory sfactory,
			final ProtocolInputStreamFactory isFactory, final SSLContext context) throws IOException {

		if (!SSL_ENABLED)
			throw new UnsupportedOperationException("SSL has not been enabled");

		synchronized (this.innerRun) {
			if (this.clients.containsKey(connection.getPeerAddress())) {
				logger.warn("Attempt to create duplicate client session to the same address: {}", connection.getPeerAddress());
				throw new IllegalStateException("Attempt to create duplicate client session to the same address: "
						+ connection.getPeerAddress());
			}

			final SocketChannel sock = SSLSocketChannel.open(SocketChannel.open(), context, this.executorService, null);
			return this.startClient(sock, connection, sfactory, isFactory);
		}
	}

	/**
	 * Requests to stop dispatchers run() method. This method wakes up the selector, even if there are no selectedKeys
	 * to stop blocking the thread.
	 */
	public void stop() {
		logger.debug("Requested stop of the Dispatcher.");
		this.requestStop = true;
		this.innerRun.selector.wakeup();
		try {
			this.innerThread.join();
		} catch (final InterruptedException e) {
			logger.error("Stopping interrupted.", e);
		}

		this.executorService.shutdown();
	}

	/**
	 * Removes given server from list of servers created by this dispatcher.
	 *
	 * @param server to be removed
	 */
	void removeServer(final ProtocolServer server) {
		this.servers.remove(server.getAddress());
		logger.trace("Server removed.");
	}

	/**
	 * Reads from socket and sends data to session through Piped Streams.
	 *
	 * @param key selection key that was marked as ready to read from
	 * @return true if the read has encountered end of channel (so no data will ever come) false if the method did read
	 *         all of its input
	 * @throws IOException if there was some error with IO streams
	 */
	private boolean read(final SelectionKey key, final long deadline) throws IOException {
		logger.trace("Started reading.");
		final SocketChannel chan = (SocketChannel) key.channel();
		final SessionStreams streams = (SessionStreams) key.attachment();
		final ProtocolInputStream pcepis = streams.getProtocolInputStream();
		final PipedOutputStream pos = streams.getPipedOutputStream();
		final PipedInputStream pis = streams.getPipedInputStream();
		final ProtocolSession session = streams.getSession();

		try {
			final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
			int r = chan.read(byteBuffer);

			while (r != 0) {
				byteBuffer.flip();

				// if we have some unread data in the buffer
				while (byteBuffer.hasRemaining()) {
					final int pisFree = session.maximumMessageSize() - pis.available();
					if (pisFree == 0)
						throw new IOException("Protocol failed to detect no-progress situation");

					int toMove = byteBuffer.remaining();

					// Do not try to write more than the input stream can accept
					if (toMove > pisFree)
						toMove = pisFree;

					// Write to the output stream and adjust buffer position
					pos.write(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), toMove);
					byteBuffer.position(byteBuffer.position() + toMove);

					// Notify input stream that it can read more stuff
					pos.flush();

					// process any messages which became available
					while (pcepis.isMessageAvailable()) {
						// read and parse message
						final ProtocolMessage msg = pcepis.getMessage();
						// send it to session for handling
						session.handleMessage(msg);
					}
				}
				byteBuffer.clear();

				/*
				 * We reached end-of-input stream. Notify close the output stream
				 * and notify the user. He is then supposed to close the session,
				 * releasing the write-end of things.
				 */
				if (r == -1) {
					logger.warn("End of input stream reached.");
					/*
					 * The input stream has some bytes, but no others are coming
					 * in. This means it should have been a complete message,
					 * but is not -> that's a malformed message.
					 */
					if (pis.available() != 0) {
						logger.warn("Received incomplete message.");
						throw new DeserializerException("Incomplete message at the end of input stream");
					}
					key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
					session.endOfInput();
					return true;
				}

				if (!chan.isOpen())
					return true;

				final long now = System.nanoTime();
				if (deadline <= now) {
					logger.trace("Read service time exceeded by {} nanoseconds.", now - deadline);
					break;
				}

				r = chan.read(byteBuffer);
			}
		} catch (final DeserializerException e) {
			// An unrecoverable malformed message has been received. Notify
			// session to take care of the fallout.
			logger.warn("Malformed message {}", e.getMessage(), e);
			session.handleMalformedMessage(e);
		} catch (final DocumentedException e) {
			// A potentially recoverable malformed message has been received.
			// Push it to the session, it will take care of the details.
			logger.warn("Malformed message {}", e.getMessage(), e);
			session.handleMalformedMessage(e);
		} catch (final RuntimeException e) {
			logger.error("Unrecoverable internal session error: {}", e.getMessage(), e);
			throw new IOException("Unrecoverable internal session error", e);
		}
		return false;
	}

	/**
	 * Writes data from ProtocolOutputStream to socket.
	 *
	 * @param key selection key that was marked as ready to write from
	 * @return false if the writing was not successful true if the queue of messages became empty
	 * @throws IOException if there was some error with the IO streams
	 */
	private boolean write(final SelectionKey key, final long deadline) throws IOException {
		logger.trace("Started writing.");

		// TODO: promote to hard error?
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isConnected()) {
			logger.warn("Channel is not connected yet.");
			return false;
		}

		final SessionStreams streams = (SessionStreams) key.attachment();
		final Queue<ByteBuffer> queue = streams.getSession().getStream().getBuffers();

		synchronized (queue) {
			logger.trace("Synchronized writing started.");
			// Write until there's not more data
			while (!queue.isEmpty()) {
				final ByteBuffer buf = queue.element();
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					/*
					 * If there is not enough space in the socket to write all the data
					 * stay in writing mode and attempt to write after the next select()
					 * call
					 */
					logger.trace("Socket queue full.");
					return false;
				}
				queue.remove();

				final long now = System.nanoTime();
				if (deadline <= now) {
					logger.trace("Write service time exceeded by {} nanoseconds.", now - deadline);
					return false;
				}
			}
			logger.trace("Write queue empty.");
			return true;
		}
	}

	private void acceptChannel(final ProtocolServer server, final SocketChannel socketChannel, final InetSocketAddress clientAddress)
			throws IOException {
		socketChannel.configureBlocking(false);

		final ProtocolSession s = server.createSession(this.stateTimer, clientAddress);
		final PipedOutputStream pos = new PipedOutputStream();
		final PipedInputStream pis = new PipedInputStream(pos, s.maximumMessageSize());
		final ProtocolInputStream inputStream = server.createInputStream(pis, s.getMessageFactory());

		final SelectionKey skey = socketChannel.register(this.innerRun.selector, SelectionKey.OP_READ);
		skey.attach(new SessionStreams(pos, pis, inputStream, s, null));
		this.sessionKeys.put(s, skey);

		// FIXME: catch RuntimeExceptions here, undo the put/attach above?
		// or can we move the .put() after this call?
		s.startSession();
	}

	/**
	 * Accepts incoming connection from a client to one of the running servers.
	 *
	 * @param key selection key that was marked as ready to accept connections
	 * @throws IOException if there was some error with IO streams
	 */
	private void accept(final SelectionKey key) throws IOException {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		final SocketChannel socketChannel = serverSocketChannel.accept();
		if (socketChannel == null)
			return;

		final InetSocketAddress clientAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
		logger.info("Requested connection for: {}", clientAddress.getAddress().getHostAddress());

		try {
			this.acceptChannel((ProtocolServer) key.attachment(), socketChannel, clientAddress);
		} catch (final Exception e) {
			logger.warn("Failed to start protocol session", e);
			socketChannel.close();
		}
	}

	/**
	 * Finishes connection of the client to the server. Starts session.
	 *
	 * @param key selection key that was marked as ready to finish connection
	 */
	private boolean finishConnection(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		final SessionStreams streams = (SessionStreams) key.attachment();
		logger.trace("Finishing connection for key {}", key);
		try {
			if (socketChannel.finishConnect()) {
				key.interestOps(SelectionKey.OP_READ);
				streams.getSession().startSession();
			}
		} catch (final IOException e) {
			this.connectFailed(key, e);
			return false;
		}
		return true;
	}

	/**
	 * Closes channel and cancels key assigned to given session.
	 *
	 * @param session session that was closed
	 */
	void closeSessionSockets(final ProtocolSession session) {
		synchronized (this.innerRun) {
			logger.debug("Trying to close sesion.");
			final SelectionKey key = this.sessionKeys.get(session);
			if (key != null) {

				try {
					key.channel().close();
				} catch (final IOException e) {
					logger.error("Session channel could not be closed.");
				} finally {
					final SessionStreams streams = (SessionStreams) key.attachment();
					if (streams.timer != null) {
						streams.timer.cancel();
						streams.timer = null;
					}

					logger.trace("Cancelling key.");
					key.cancel();

					final PipedOutputStream pos = streams.getPipedOutputStream();
					try {
						pos.close();
					} catch (final IOException e) {
						logger.error("Session-internal output stream could not be closed.");
					} finally {
						final PipedInputStream pis = streams.getPipedInputStream();
						try {
							pis.close();
						} catch (final IOException e) {
							logger.error("Session-internal input stream could not be closed.");
						}
					}
				}
			}
			this.sessionKeys.remove(key);
			logger.debug("Session sockets closed.");
		}
	}

	@Override
	public void onSessionClosed(final ProtocolSession session) {
		synchronized (this.innerRun) {
			this.innerRun.selector.wakeup();
			this.closeSessionSockets(session);
			this.clients.inverse().remove(session);
			logger.debug("Session {} removed.", session);
		}
	}

	@Override
	public void checkOutputBuffer(final ProtocolSession session) {
		final SelectionKey key = this.sessionKeys.get(session);
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		key.selector().wakeup();
	}

	@Override
	public void close() throws IOException {
		for (final Entry<InetSocketAddress, ProtocolServer> s : this.servers.entrySet()) {
			s.getValue().close();
		}
		for (final Entry<InetSocketAddress, ProtocolSession> s : this.clients.entrySet()) {
			s.getValue().close();
		}
	}

	/**
	 * Gets milliseconds between reconnects.
	 * @return time in milliseconds between reconnects
	 */
	public synchronized int getReconnectMillis() {
		return this.reconnectMillis;
	}

	/**
	 * Sets milliseconds between reconnects.
	 * @param reconnectMillis new value
	 */
	public synchronized void setReconnectMillis(final int reconnectMillis) {
		Preconditions.checkArgument(reconnectMillis > 0, "Reconnect milliseconds value has to be positive");
		this.reconnectMillis = reconnectMillis;
		// FIXME: readjust all pending timers
	}

	/**
	 * Gets maximum tries for connection.
	 * @return max connection count
	 */
	public synchronized int getMaxConnectCount() {
		return this.maxConnectCount;
	}

	/**
	 * Sets maximum tries for connection.
	 * @param maxConnectCount new value
	 */
	public synchronized void setMaxConnectCount(final int maxConnectCount) {
		this.maxConnectCount = maxConnectCount;
		// FIXME: purge all sessions which already exceed the limit
	}

	public synchronized int getServiceMillis() {
		return this.serviceMillis;
	}

	public synchronized void setServiceMillis(final int serviceMillis) {
		Preconditions.checkArgument(serviceMillis > 0);
		this.serviceMillis = serviceMillis;
	}

	/**
	 * Gets thread factory.
	 * @return thread factory
	 */
	public ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}
}
