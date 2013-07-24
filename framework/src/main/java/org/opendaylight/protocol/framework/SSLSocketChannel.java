/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Base class for an SSL-enabled socket channel. It is completed as
 * SSLSocketChannel in one of the two Java version-specific files.
 */
final class SSLSocketChannel extends SocketChannel implements SSLSelectableChannel {
	private enum InternalState {
		/**
		 * Freshly created socket. Must be connected.
		 */
		IDLE,
		/**
		 * Underlying TCP connection is being established.
		 */
		CONNECTING,
		/**
		 * Underlying TCP connection is established, we are currently
		 * negotiating SSL session parameters.
		 */
		NEGOTIATING,
		/**
		 * Connection attempt has been resolved, we need the user
		 * to call finishConnect().
		 */
		CONNECT_RESOLVED,
		/**
		 * We have notified the user that the connection has failed,
		 * all we need to do is cleanup resources.
		 */
		CONNECT_FAILED,
		/**
		 * We have notified user that the connection has been
		 * established and the channel is fully operational.
		 */
		CONNECTED,
		/**
		 * We are closing down the channel. From user's perspective
		 * it is already dead, we just need to cleanup.
		 */
		CLOSING,
		/**
		 * The channel has been closed and all resources released.
		 */
		CLOSED,
	}

	private static final Logger logger = LoggerFactory.getLogger(SSLSocketChannel.class);
	private SSLServerSocketChannel parent;
	final SocketChannel channel;
	final SSLEngine engine;

	private final ByteBuffer fromNetwork, toNetwork, toUser;
	private final ByteBuffer empty = ByteBuffer.allocate(0);
	private final Executor executor;
	private IOException connectResult = null;
	private IOException writeFailed = null, closeFailed = null;
	private boolean readDone = false;
	private boolean closedInput = false, closedOutput = false;
	private InternalState state;

	private SSLSocketChannel(final SocketChannel channel, final SSLEngine engine,
			final Executor executor, final SSLServerSocketChannel parent) throws SSLException {
		super(channel.provider());
		this.executor = executor;
		this.channel = channel;
		this.parent = parent;
		this.engine = engine;

		final SSLSession session = engine.getSession();
		fromNetwork = ByteBuffer.allocate(session.getPacketBufferSize());
		fromNetwork.limit(0);
		toNetwork = ByteBuffer.allocate(session.getPacketBufferSize());
		toNetwork.limit(0);
		toUser = ByteBuffer.allocate(session.getApplicationBufferSize());

		if (parent != null) {
			engine.setUseClientMode(false);
			engine.setWantClientAuth(true);
			engine.setNeedClientAuth(false);
			engine.beginHandshake();
			state = InternalState.NEGOTIATING;
		} else
			state = InternalState.IDLE;
	}

	public static SSLSocketChannel open(final SocketChannel channel, final SSLContext context,
			final Executor executor, final SSLServerSocketChannel parent) throws IOException {

		return new SSLSocketChannel(channel, context.createSSLEngine(), executor, parent);
	}

	@Override
	public synchronized boolean connect(final SocketAddress remote) throws IOException {
		switch (state) {
		case CLOSED:
		case CLOSING:
		case CONNECT_FAILED:
			throw new ClosedChannelException();
		case CONNECTED:
			throw new AlreadyConnectedException();
		case CONNECTING:
		case CONNECT_RESOLVED:
		case NEGOTIATING:
			throw new ConnectionPendingException();
		case IDLE:
			if (channel.connect(remote)) {
				engine.setUseClientMode(true);
				engine.beginHandshake();
				state = InternalState.NEGOTIATING;
			} else
				state = InternalState.CONNECTING;
			return false;
		}

		throw new IllegalStateException("Unhandled state " + state);
	}

	@Override
	public synchronized boolean finishConnect() throws IOException {
		logger.trace("Attempting to finish connection in state {}", state);

		switch (state) {
		case CLOSED:
		case CLOSING:
		case CONNECT_FAILED:
			throw new ClosedChannelException();
		case CONNECTED:
			return true;
		case CONNECT_RESOLVED:
			if (connectResult != null) {
				state = InternalState.CONNECT_FAILED;
				try {
					logger.trace("Internal close after failed connect");
					close();
				} catch (IOException e) {
					logger.trace("Failed to invoked internal close", e);
				}
				throw connectResult;
			}

			state = InternalState.CONNECTED;
			if (parent != null) {
				parent.addNewChannel(this);
				parent = null;
			}
			return true;
		case CONNECTING:
		case NEGOTIATING:
			return false;
		case IDLE:
			throw new NoConnectionPendingException();
		}

		throw new IllegalStateException("Unhandled state " + state);
	}

	@Override
	public synchronized boolean isConnected() {
		return state == InternalState.CONNECTED;
	}

	@Override
	public synchronized boolean isConnectionPending() {
		switch (state) {
		case CONNECTING:
		case CONNECT_RESOLVED:
		case NEGOTIATING:
			return true;
		default:
			return false;
		}
	}

	private int readNetwork() throws IOException {
		fromNetwork.compact();

		final int ret;
		try {
			ret = channel.read(fromNetwork);
		} finally {
			fromNetwork.flip();
		}

		logger.trace("Channel {} has input {} after {}", this, fromNetwork.remaining(), ret);
		return ret;
	}

	private int writeNetwork() throws IOException {
		toNetwork.flip();

		final int ret;
		try {
			ret = channel.write(toNetwork);
		} finally {
			toNetwork.compact();
		}

		logger.trace("Channel {} has output {} after {}", this, toNetwork.remaining(), ret);
		return ret;
	}

	private void checkChannelState() throws IOException {
		switch (state) {
		case CLOSED:
		case CLOSING:
		case CONNECT_FAILED:
			throw new ClosedChannelException();
		case CONNECTED:
			break;
		case CONNECT_RESOLVED:
		case CONNECTING:
		case IDLE:
		case NEGOTIATING:
			throw new NotYetConnectedException();
		}
	}

	private boolean checkReadState() throws IOException {
		checkChannelState();
		return closedInput;
	}

	@Override
	public synchronized int read(final ByteBuffer dst) throws IOException {
		if (checkReadState())
			return -1;

		/*
		 * If we have some data overflowed from negotiation, flush that
		 * first.
		 */
		if (toUser.position() != 0) {
			logger.trace("toUser has {}", toUser.position());
			toUser.flip();

			final int xfer = Math.min(toUser.remaining(), dst.remaining());
			dst.put(toUser.array(), toUser.arrayOffset() + toUser.position(), xfer);
			toUser.position(toUser.position() + xfer);
			toUser.compact();
			return xfer;
		}

		// We have input data, unwrap it
		if (fromNetwork.hasRemaining()) {
			final SSLEngineResult res = engine.unwrap(fromNetwork, dst);
			return res.bytesProduced();
		}

		// EOF on underlying stream, inform the engine
		if (readDone)
			engine.closeInbound();

		// SSL engine says there may be some more input
		if (!engine.isInboundDone())
			return 0;

		logger.trace("SSL engine indicates clean shutdown");
		return -1;
	}

	@Override
	public synchronized long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
		if (checkReadState())
			return -1;

		/*
		 * Search for the first buffer with available data and perform
		 * a single-buffer read into it. Not completely efficient, but
		 * does the work required.
		 */
		for (int i = offset; i < length; ++i)
			if (dsts[i].remaining() != 0)
				return read(dsts[i]);

		return 0;
	}

	@Override
	public Socket socket() {
		// We do not support this operation, everyone should use Java 7 interfaces
		throw new UnsupportedOperationException("SSLSocketChannel does not provide a fake Socket implementation");
	}

	private void checkWriteState() throws IOException {
		checkChannelState();

		if (closedOutput)
			throw new ClosedChannelException();

		if (writeFailed != null)
			throw writeFailed;
	}

	@Override
	public synchronized int write(final ByteBuffer src) throws IOException {
		checkWriteState();

		final SSLEngineResult res = engine.wrap(src, toNetwork);
		return res.bytesConsumed();
	}

	@Override
	public synchronized long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
		checkWriteState();

		final SSLEngineResult res = engine.wrap(srcs, offset, length, toNetwork);
		return res.bytesConsumed();
	}

	@Override
	protected synchronized void implCloseSelectableChannel() throws IOException {
		logger.trace("Closing channel in state {}", state);

		switch (state) {
		case CONNECTED:
			state = InternalState.CLOSING;
			engine.closeOutbound();
			break;
		case CLOSED:
		case CLOSING:
			// Nothing to do
			break;
		case CONNECT_FAILED:
		case CONNECT_RESOLVED:
		case CONNECTING:
		case IDLE:
		case NEGOTIATING:
			state = InternalState.CLOSED;
			channel.close();
			break;
		}
	}

	@Override
	protected void implConfigureBlocking(final boolean block) throws IOException {
		channel.configureBlocking(block);
	}

	@Override
	public synchronized int computeInterestOps(int userOps) {
		logger.trace("Interestops in state {} userOps {}", state, userOps);

		int ret = 0;

		switch (state) {
		case CLOSED:
		case CONNECT_FAILED:
		case CONNECT_RESOLVED:
		case IDLE:
			return 0;
		case CLOSING:
			if (engine.isOutboundDone() && toNetwork.position() == 0)
				throw new IllegalStateException("Network flush completed, but still in CLOSING state");
			return SelectionKey.OP_WRITE;
		case CONNECTING:
			return SelectionKey.OP_CONNECT;
		case NEGOTIATING:
			userOps = 0;

			final HandshakeStatus st = engine.getHandshakeStatus();

			logger.trace("SSL Engine status {}", st);

			switch (st) {
			case NEED_UNWRAP:
				userOps = SelectionKey.OP_READ;
				break;
			case NEED_WRAP:
				userOps = SelectionKey.OP_WRITE;
				break;
			default:
				logger.trace("Unexpected SSLEngine handshake status {}", st);
				connectResult = new IOException("Unexpected SSLEngine handshake status " + st);
				connectResult.fillInStackTrace();
				state = InternalState.CONNECT_RESOLVED;
				return 0;
			}

			// Intentional fall through
		case CONNECTED:
			if ((userOps & SelectionKey.OP_READ) != 0 && !fromNetwork.hasRemaining())
				ret |= SelectionKey.OP_READ;
			if ((userOps & SelectionKey.OP_WRITE) != 0 && !toNetwork.hasRemaining())
				ret |= SelectionKey.OP_WRITE;

			logger.trace("userOps {} fromNetwork {} toNetwork {} ret {}", userOps, fromNetwork.remaining(), toNetwork.remaining(), ret);
			return ret;
		}

		throw new IllegalStateException("Unhandled state " + state);
	}

	private void performIO() {
		logger.trace("IO operations in state {}", state);

		switch (state) {
		case CLOSED:
		case CONNECT_RESOLVED:
		case IDLE:
			// Nothing to do
			break;
		case CLOSING:
			boolean forceClose = false;
			if (!engine.isOutboundDone()) {
				try {
					engine.wrap(empty, toNetwork);
				} catch (SSLException e) {
					logger.trace("Failed to close down SSL engine outbound", e);
				}
			}

			if (toNetwork.position() != 0) {
				try {
					writeNetwork();
				} catch (IOException e) {
					logger.trace("Failed to flush outstanding buffers, forcing close", e);
					forceClose = true;
				}
			}

			if (forceClose || (engine.isOutboundDone() && toNetwork.position() == 0)) {
				logger.trace("Completed state flush");
				state = InternalState.CLOSED;
				try {
					channel.close();
				} catch (IOException e) {
					logger.trace("Failed to close slave channel", e);
				}
			}
			break;
		case CONNECT_FAILED:
			try {
				logger.trace("Invoking internal close after failure");
				close();
			} catch (IOException e) {
				logger.trace("Internal fail closed", e);
			}
			break;
		case CONNECTED:
			try {
				if (!readDone && readNetwork() < 0) {
					readDone = true;
					try {
						engine.closeInbound();
					} catch (IOException e) {
						logger.trace("TLS reported close error", e);
						closeFailed = e;
					}
				}
			} catch (IOException e) {
				logger.trace("Background read failed", e);
				readDone = true;
			}

			try {
				if (toNetwork.position() != 0)
					writeNetwork();
			} catch (IOException e) {
				logger.trace("Background write failed", e);
				writeFailed = e;
				toNetwork.clear();
			}
			break;
		case CONNECTING:
			try {
				if (channel.finishConnect()) {
					engine.setUseClientMode(true);
					engine.beginHandshake();
					state = InternalState.NEGOTIATING;
				}
			} catch (IOException e) {
				logger.trace("Finished connection with error", e);
				connectResult = e;
				state = InternalState.CONNECT_RESOLVED;
			}
			break;
		case NEGOTIATING:
			boolean needMore = true;

			do {
				final HandshakeStatus st = engine.getHandshakeStatus();
				if (st == HandshakeStatus.NEED_TASK) {
					// Dispatch any blocking tasks that SSLEngine has for us.
					while (true) {
						final Runnable r = engine.getDelegatedTask();
						if (r == null)
							break;

						executor.execute(r);
					}
					continue;
				}

				try {
					if (readNetwork() < 0) {
						logger.trace("Unexpected end of stream during negotiation");
						connectResult = new EOFException("Unexpected end-of-channel during SSL negotiation");
						connectResult.fillInStackTrace();
						state = InternalState.CONNECT_RESOLVED;
						break;
					}
					writeNetwork();
				} catch (IOException e) {
					logger.trace("IO error during SSL negotiation", e);
					connectResult = e;
					state = InternalState.CONNECT_RESOLVED;
					break;
				}

				final SSLEngineResult res;
				try {
					logger.trace("Status {} fromNetwork {} toNetwork {} toUser {}", st, fromNetwork.remaining(), toNetwork.remaining(), toUser.remaining());

					if (st == HandshakeStatus.NEED_UNWRAP) {
						// SSLEngine needs to read some data from the network
						res = engine.unwrap(fromNetwork, toUser);
						if (res.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW)
							needMore = false;
					} else if (st == HandshakeStatus.NEED_WRAP) {
						// SSLEngine needs to write some data to the network
						res = engine.wrap(empty, toNetwork);
						if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)
							needMore = false;
					} else {
						logger.trace("Unexpected state {} in SSL negotiation", engine.getHandshakeStatus());
						connectResult = new IOException("Unexpected SSL negotiation state");
						connectResult.fillInStackTrace();
						state = InternalState.CONNECT_RESOLVED;
						break;
					}
				} catch (SSLException e) {
					logger.trace("SSL negotiation failed", e);
					connectResult = e;
					state = InternalState.CONNECT_RESOLVED;
					break;
				}

				logger.trace("SSL needMore {} result {}", needMore, res);

				if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
					final SSLSession s = engine.getSession();
					logger.trace("SSL session established: {}", s);
					state = InternalState.CONNECT_RESOLVED;
					break;
				}
			} while (needMore);
		}
	}

	@Override
	public synchronized int computeReadyOps() {
		performIO();

		logger.trace("Readyops in state {}", state);

		switch (state) {
		case CLOSED:
		case CLOSING:
		case CONNECT_FAILED:
		case CONNECTING:
		case IDLE:
		case NEGOTIATING:
			return 0;
		case CONNECT_RESOLVED:
			return SelectionKey.OP_CONNECT;
		case CONNECTED:
			int ret = 0;

			if (toNetwork.hasRemaining() || writeFailed != null)
				ret |= SelectionKey.OP_WRITE;
			if (fromNetwork.hasRemaining() || toUser.position() != 0)
				ret |= SelectionKey.OP_READ;

			return ret;
		}

		throw new IllegalStateException("Unhandled state " + state);
	}

	@Override
	public SocketChannel bind(final SocketAddress local) throws IOException {
		channel.bind(local);
		return this;
	}

	@Override
	public SocketAddress getLocalAddress() throws IOException {
		return channel.getLocalAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() throws IOException {
		return channel.getRemoteAddress();
	}

	@Override
	public synchronized SocketChannel shutdownInput() throws IOException {
		checkChannelState();

		if (!closedInput) {
			closedInput = true;
			if (closeFailed != null)
				throw closeFailed;
			logger.debug("Socket {} input shut down", this);
		}

		return this;
	}

	@Override
	public synchronized SocketChannel shutdownOutput() throws IOException {
		checkChannelState();

		if (!closedOutput) {
			closedOutput = true;
			engine.closeOutbound();
			logger.debug("Socket {} output shut down", this);
		}

		return this;
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return channel.getOption(name);
	}

	@Override
	public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
		channel.setOption(name, value);
		return this;
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return channel.supportedOptions();
	}

	synchronized boolean hasParent() {
		return parent != null;
	}
}

