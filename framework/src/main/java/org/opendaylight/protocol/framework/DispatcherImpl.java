/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.Closeable;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public final class DispatcherImpl implements Closeable, Dispatcher {

	private static final Logger logger = LoggerFactory.getLogger(DispatcherImpl.class);

	private final EventLoopGroup bossGroup;

	private final EventLoopGroup workerGroup;

	public DispatcherImpl() {
		// FIXME: we should get these as arguments
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
	}

	@Override
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> ChannelFuture createServer(
			final InetSocketAddress address, final SessionListenerFactory<L> listenerFactory,
			final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final ProtocolMessageFactory<M> messageFactory) {
		final ServerBootstrap b = new ServerBootstrap();
		b.group(this.bossGroup, this.workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.option(ChannelOption.SO_BACKLOG, 128);
		b.childHandler(new ChannelInitializerImpl<M, S, L>(negotiatorFactory,
				listenerFactory, new ProtocolHandlerFactory<M>(messageFactory), new DefaultPromise<S>(GlobalEventExecutor.INSTANCE)));
		b.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		final ChannelFuture f = b.bind(address);
		logger.debug("Initiated server {} at {}.", f, address);
		return f;

	}

	@Override
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> Future<S> createClient(
			final InetSocketAddress address, final L listener, final SessionNegotiatorFactory<M, S, L> negotiatorFactory,
			final ProtocolMessageFactory<M> messageFactory,	final ReconnectStrategy strategy) {
		final ProtocolSessionPromise<M, S, L> p = new ProtocolSessionPromise<M, S, L>(workerGroup, address, negotiatorFactory,
				new SessionListenerFactory<L>() {
			private boolean created = false;

			@Override
			public synchronized L getSessionListener() {
				Preconditions.checkState(created == false);
				created = true;
				return listener;
			}

		}, new ProtocolHandlerFactory<M>(messageFactory), strategy);

		p.connect();
		logger.debug("Client created.");
		return p;
	}

	@Override
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> Future<Void> createReconnectingClient(
			final InetSocketAddress address, final L listener, final SessionNegotiatorFactory<M, S, L> negotiatorFactory,
			final ProtocolMessageFactory<M> messageFactory, final ReconnectStrategyFactory connectStrategyFactory,
			final ReconnectStrategy reestablishStrategy) {

		final ReconnectPromise<M, S, L> p = new ReconnectPromise<M, S, L>(this, address, listener, negotiatorFactory,
				messageFactory, connectStrategyFactory, reestablishStrategy);

		p.connect();

		return p;

	}

	@Override
	public void close() {
		try {
			this.workerGroup.shutdownGracefully();
		} finally {
			this.bossGroup.shutdownGracefully();
		}
	}
}
