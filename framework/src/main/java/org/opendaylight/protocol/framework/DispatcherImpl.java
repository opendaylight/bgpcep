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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
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
import java.util.Map;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public final class DispatcherImpl implements Dispatcher, SessionParent {

	final class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

		private final ProtocolServer server;

		private ProtocolSession session;

		public ServerChannelInitializer(final ProtocolServer server) {
			this.server = server;
		}

		@Override
		protected void initChannel(final SocketChannel ch) throws Exception {
			final ProtocolHandlerFactory factory = new ProtocolHandlerFactory(DispatcherImpl.this.messageFactory);
			final ChannelHandler handler = factory.getSessionOutboundHandler();
			ch.pipeline().addFirst("outbound", handler);
			ch.pipeline().addFirst("decoder", factory.getDecoder());
			this.session = this.server.createSession(DispatcherImpl.this.stateTimer, ch);

			ch.pipeline().addAfter("decoder", "inbound", factory.getSessionInboundHandler(this.session));
			ch.pipeline().addAfter("inbound", "encoder", factory.getEncoder());
		}

		public ProtocolSession getSession() {
			return this.session;
		}

	}

	final class ClientChannelInitializer<T extends ProtocolSession> extends ChannelInitializer<SocketChannel> {

		private final ProtocolSessionFactory<T> sfactory;

		private final ProtocolConnection connection;

		private T session;

		public ClientChannelInitializer(final ProtocolConnection connection, final ProtocolSessionFactory<T> sfactory) {
			this.connection = connection;
			this.sfactory = sfactory;
		}

		@Override
		protected void initChannel(final SocketChannel ch) throws Exception {
			final ProtocolHandlerFactory factory = new ProtocolHandlerFactory(DispatcherImpl.this.messageFactory);
			final ChannelHandler handler = factory.getSessionOutboundHandler();
			ch.pipeline().addFirst("outbound", handler);
			ch.pipeline().addFirst("decoder", factory.getDecoder());
			this.session = this.sfactory.getProtocolSession(DispatcherImpl.this, DispatcherImpl.this.stateTimer, this.connection, 0,
					ch.pipeline().context(ProtocolSessionOutboundHandler.class));
			ch.pipeline().addAfter("decoder", "inbound", factory.getSessionInboundHandler(this.session));
			ch.pipeline().addAfter("inbound", "encoder", factory.getEncoder());
		}

		public T getSession() {
			return this.session;
		}
	}

	static final class ProtocolServerPromise extends DefaultPromise<ProtocolServer> {
		private final ChannelFuture cf;

		ProtocolServerPromise(final ChannelFuture cf) {
			super();
			this.cf = cf;
		}

		@Override
		public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
			this.cf.cancel(mayInterruptIfRunning);
			return super.cancel(mayInterruptIfRunning);
		}
	}

	static final class ProtocolSessionPromise<T extends ProtocolSession> extends DefaultPromise<T> {
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

	private final ProtocolMessageFactory messageFactory;

	private final Map<ProtocolServer, Channel> serverSessions;

	private final Map<ProtocolSession, Channel> clientSessions;

	public DispatcherImpl(final ProtocolMessageFactory factory) {
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.stateTimer = new Timer();
		this.messageFactory = factory;
		this.clientSessions = Maps.newHashMap();
		this.serverSessions = Maps.newHashMap();
	}

	@Override
	public Future<ProtocolServer> createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory<?> sessionFactory) {
		final ProtocolServer server = new ProtocolServer(address, connectionFactory, sessionFactory, this);
		final ServerBootstrap b = new ServerBootstrap();
		b.group(this.bossGroup, this.workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.option(ChannelOption.SO_BACKLOG, 128);
		b.childHandler(new ServerChannelInitializer(server));
		b.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		final ChannelFuture f = b.bind(address);
		final ProtocolServerPromise p = new ProtocolServerPromise(f);

		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture cf) {
				if (cf.isSuccess()) {
					p.setSuccess(server);
					serverSessions.put(server, cf.channel());
					return;
				} else if (cf.isCancelled()) {
					p.cancel(false);
				} else
					p.setFailure(cf.cause());
			}
		});

		logger.debug("Created server {}.", server);
		return p;
	}

	@Override
	public <T extends ProtocolSession> Future<T> createClient(final ProtocolConnection connection, final ProtocolSessionFactory<T> sfactory) {
		final Bootstrap b = new Bootstrap();
		b.group(this.workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		final ClientChannelInitializer<T> init = new ClientChannelInitializer<T>(connection, sfactory);
		b.handler(init);
		final ChannelFuture f = b.connect(connection.getPeerAddress());
		final ProtocolSessionPromise<T> p = new ProtocolSessionPromise<T>(f);

		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture cf) {
				if (cf.isSuccess()) {
					final T s = init.getSession();
					p.setSuccess(s);
					clientSessions.put(s, cf.channel());
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
		logger.trace("Removing client session: {}", session);
		final Channel ch = this.clientSessions.get(session);
		ch.close();
		this.clientSessions.remove(session);
		logger.debug("Removed client session: {}", session.toString());
	}

	void onServerClosed(final ProtocolServer server) {
		logger.trace("Removing server session: {}", server);
		final Channel ch = this.serverSessions.get(server);
		ch.close();
		this.clientSessions.remove(server);
		logger.debug("Removed server session: {}", server.toString());
	}
}
