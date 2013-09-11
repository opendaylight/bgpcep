/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public abstract class AbstractDispatcher<S extends ProtocolSession<?>> implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AbstractDispatcher.class);

	private final EventLoopGroup bossGroup;

	private final EventLoopGroup workerGroup;

	protected AbstractDispatcher() {
		// FIXME: we should get these as arguments
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
	}

	public abstract void initializeChannel(SocketChannel channel, Promise<S> promise);

	/**
	 * Creates server. Each server needs factories to pass their instances to client sessions.
	 * 
	 * @param address address to which the server should be bound
	 * 
	 * @return ChannelFuture representing the binding process
	 */
	@VisibleForTesting
	public ChannelFuture createServer(final InetSocketAddress address) {
		final ServerBootstrap b = new ServerBootstrap();
		b.group(this.bossGroup, this.workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.option(ChannelOption.SO_BACKLOG, 128);
		b.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(final SocketChannel ch) throws Exception {
				initializeChannel(ch, new DefaultPromise<S>(GlobalEventExecutor.INSTANCE));
			}
		});
		b.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		final ChannelFuture f = b.bind(address);
		logger.debug("Initiated server {} at {}.", f, address);
		return f;

	}

	/**
	 * Creates a client.
	 * 
	 * @param address remote address
	 * @param connectStrategy Reconnection strategy to be used when initial connection fails
	 * 
	 * @return Future representing the connection process. Its result represents the combined success of TCP connection
	 *         as well as session negotiation.
	 */
	protected Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy) {
		final Bootstrap b = new Bootstrap();
		final ProtocolSessionPromise<S> p = new ProtocolSessionPromise<S>(address, strategy, b);
		b.group(this.workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).handler(
				new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(final SocketChannel ch) throws Exception {
						initializeChannel(ch, p);
					}
				});
		p.connect();
		logger.debug("Client created.");
		return p;
	}

	/**
	 * Creates a client.
	 * 
	 * @param address remote address
	 * @param connectStrategyFactory Factory for creating reconnection strategy to be used when initial connection fails
	 * @param reestablishStrategy Reconnection strategy to be used when the already-established session fails
	 * 
	 * @return Future representing the reconnection task. It will report completion based on reestablishStrategy, e.g.
	 *         success if it indicates no further attempts should be made and failure if it reports an error
	 */
	protected Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
			final ReconnectStrategy reestablishStrategy) {

		final ReconnectPromise<S> p = new ReconnectPromise<S>(this, address, connectStrategyFactory, reestablishStrategy);
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
