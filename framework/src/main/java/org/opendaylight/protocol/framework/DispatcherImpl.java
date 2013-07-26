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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public final class DispatcherImpl implements Dispatcher, SessionParent {

	final class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

		private final ProtocolServer server;

		public ServerChannelInitializer(final ProtocolServer server) {
			this.server = server;
		}

		@Override
		protected void initChannel(final SocketChannel ch) throws Exception {
			final ProtocolSession session = this.server.createSession(DispatcherImpl.this.stateTimer, ch);
			ch.pipeline().addLast(DispatcherImpl.this.handlerFactory.getHandlers(session));
		}

	}

	final class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

		private final ProtocolSessionFactory sfactory;

		private final ProtocolConnection connection;

		private ProtocolSession session;

		public ClientChannelInitializer(final ProtocolConnection connection, final ProtocolSessionFactory sfactory) {
			this.connection = connection;
			this.sfactory = sfactory;
		}

		@Override
		protected void initChannel(final SocketChannel ch) throws Exception {
			this.session = this.sfactory.getProtocolSession(DispatcherImpl.this, DispatcherImpl.this.stateTimer, this.connection, 0,
					ch.pipeline().context(ProtocolSessionOutboundHandler.class));
			ch.pipeline().addLast(DispatcherImpl.this.handlerFactory.getHandlers(this.session));
		}

		public ProtocolSession getSession() {
			return this.session;
		}

	}

	final class ProtocolSessionPromise extends DefaultPromise<ProtocolSession> {
		private final ChannelFuture cf;

		ProtocolSessionPromise(final ChannelFuture cf) {
			super();
			this.cf = cf;
		}

		@Override
		public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
			this.cf.cancel(mayInterruptIfRunning);
			return super.cancel(mayInterruptIfRunning);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(DispatcherImpl.class);

	private final EventLoopGroup bossGroup;

	private final EventLoopGroup workerGroup;

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	private final ProtocolHandlerFactory handlerFactory;

	public DispatcherImpl(final ProtocolMessageFactory factory) {
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.stateTimer = new Timer();
		this.handlerFactory = new ProtocolHandlerFactory(factory);
	}

	@Override
	public ProtocolServer createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sessionFactory) {
		final ProtocolServer server = new ProtocolServer(address, connectionFactory, sessionFactory);
		final ServerBootstrap b = new ServerBootstrap();
		b.group(this.bossGroup, this.workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.option(ChannelOption.SO_BACKLOG, 128);
		b.childHandler(new ServerChannelInitializer(server));
		b.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		final ChannelFuture f = b.bind(address);
		// b.localAddress(address);
		logger.debug("Server {} created.", server);
		return server;
	}

	@Override
	public Future<ProtocolSession> createClient(final ProtocolConnection connection, final ProtocolSessionFactory sfactory) {
		final Bootstrap b = new Bootstrap();
		b.group(this.workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		final ClientChannelInitializer init = new ClientChannelInitializer(connection, sfactory);
		b.handler(init);
		final ChannelFuture f = b.connect(connection.getPeerAddress());
		final ProtocolSessionPromise p = new ProtocolSessionPromise(f);

		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture cf) {
				if (cf.isSuccess()) {
					p.setSuccess(init.getSession());
					return;
				} else if (cf.isCancelled()) {
					p.cancel(false);
				} else
					p.setFailure(cf.cause());
			}
		});
		logger.debug("Client created.");
		return p;
	}

	@Override
	public void close() throws IOException {
		this.workerGroup.shutdownGracefully();
		this.bossGroup.shutdownGracefully();
	}

	@Override
	public void onSessionClosed(final ProtocolSession session) {
		// TODO Auto-generated method stub
	}
}
