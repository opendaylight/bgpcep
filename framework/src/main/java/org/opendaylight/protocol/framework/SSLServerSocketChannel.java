/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;

/**
 * SSL-enabled equivalent of ServerSocketChannel. This class uses a backend
 * ServerSocketChannel to implement network functionality. Each instance is
 * bound to a SSLContext, which is used to create a per-connection SSLEngine,
 * which is encapsulated into the returned SSLSocketChannel.
 */
final class SSLServerSocketChannel extends ServerSocketChannel implements SSLSelectableChannel {
	private static final Logger logger = LoggerFactory.getLogger(SSLServerSocketChannel.class);
	private final Queue<SocketChannel> newChannels = Queues.newArrayDeque();
	private final SSLContext context;
	private final Executor executor;
	private final Selector selector;
	private boolean closed = false;

	protected final ServerSocketChannel channel;

	private SSLServerSocketChannel(final Selector selector, final ServerSocketChannel channel, final SSLContext context, final Executor executor) {
		super(channel.provider());
		this.selector = selector;
		this.executor = executor;
		this.channel = channel;
		this.context = context;
	}

	public static SSLServerSocketChannel open(final Selector selector, final SSLContext context, final Executor executor) throws IOException {
		return new SSLServerSocketChannel(selector, ServerSocketChannel.open(), context, executor);
	}

	@Override
	public final synchronized SocketChannel accept() {
		return newChannels.poll();
	}

	@Override
	public ServerSocket socket() {
		// We do not support this operation, everyone should use Java 7 interfaces
		throw new UnsupportedOperationException("SSLSocketChannel does not provide a fake Socket implementation");
	}

	@Override
	protected synchronized void implCloseSelectableChannel() throws IOException {
		closed = true;
		while (!newChannels.isEmpty()) {
			final SocketChannel c = newChannels.poll();
			try {
				c.close();
			} catch (IOException e) {
				logger.trace("Failed to close a queued channel", e);
			}
		}
		channel.close();
	}

	@Override
	protected void implConfigureBlocking(final boolean block) throws IOException {
		channel.configureBlocking(block);
	}

	@Override
	public final synchronized int computeInterestOps(final int ops) {
		// We are always interested in accepting stuff
		return SelectionKey.OP_ACCEPT;
	}

	private void performIO() {
		while (true) {
			final SocketChannel newchan;
			try {
				newchan = channel.accept();
				if (newchan == null)
					break;
			} catch (IOException e) {
				logger.trace("Underlying accept() failed", e);
				return;
			}

			try {
				final SocketChannel sc;
				try {
					sc = SSLSocketChannel.open(newchan, context, executor, this);
				} catch (IOException e) {
					logger.trace("Failed to create SSL channel", e);
					newchan.close();
					continue;
				}

				try {
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_CONNECT, null);
				} catch (IOException e) {
					logger.trace("Failed to register SSL channel", e);
					sc.close();
					continue;
				}

				logger.trace("Accepted new connection, channel is {} backend {}", sc, newchan);
			} catch (IOException e1) {
				logger.trace("Failed to close failed channel", e1);
			}
		}
	}

	@Override
	public final synchronized int computeReadyOps() {
		if (closed)
			return 0;

		performIO();

		// We need to be non-closed and have enqueue channels to be ready
		if (!closed && !newChannels.isEmpty())
			return SelectionKey.OP_ACCEPT;
		return 0;
	}

	/**
	 * Enqueue a freshly-established child channel for reporting as
	 * a ready-to-accept connection.
	 *
	 * @param channel Fresh channel, expected to be in connected state
	 */
	final synchronized void addNewChannel(final SocketChannel channel) {
		if (closed) {
			try {
				channel.close();
			} catch (IOException e) {
				logger.trace("Failed to close a queued channel", e);
			}
		} else
			newChannels.add(channel);
	}

	@Override
	public SocketAddress getLocalAddress() throws IOException {
		return channel.getLocalAddress();
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return channel.getOption(name);
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return channel.supportedOptions();
	}

	@Override
	public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
		channel.bind(local, backlog);
		return this;
	}

	@Override
	public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
		channel.setOption(name, value);
		return this;
	}
}

