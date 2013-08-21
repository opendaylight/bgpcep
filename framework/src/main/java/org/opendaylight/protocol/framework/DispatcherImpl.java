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
import io.netty.util.concurrent.FutureListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Timer;

import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
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

		T getSession() {
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
					synchronized (DispatcherImpl.this.serverSessions) {
						DispatcherImpl.this.serverSessions.put(server, cf.channel());
					}
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

	private final class ConnectionRetryListener<T extends ProtocolSession> implements ChannelFutureListener {
		private final ClientChannelInitializer<T> init;
		private final ProtocolConnection connection;
		private final ReconnectStrategy strategy;
		private final Bootstrap b;

		private final DefaultPromise<T> promise = new DefaultPromise<T>() {
			@Override
			public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
				cancelConnection(mayInterruptIfRunning);
				return super.cancel(mayInterruptIfRunning);
			}
		};

		@GuardedBy("this")
		private Future<?> pending;

		ConnectionRetryListener(final ProtocolConnection connection, final ProtocolSessionFactory<T> sfactory, final ReconnectStrategy strategy) {
			this.connection = Preconditions.checkNotNull(connection);
			this.strategy = Preconditions.checkNotNull(strategy);

			init = new ClientChannelInitializer<T>(connection, sfactory);
			b = new Bootstrap();
			b.group(workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).handler(init);
		}

		@Override
		public synchronized void operationComplete(ChannelFuture future) throws Exception {
			Preconditions.checkState(pending == future);

			/*
			 * Triggered when a connection attempt is resolved.
			 */
			if (future.isSuccess()) {
				final T s = init.getSession();
				promise.setSuccess(s);
				strategy.reconnectSuccessful();
				synchronized (DispatcherImpl.this.clientSessions) {
					DispatcherImpl.this.clientSessions.put(s, future.channel());
				}
			} else {

				final Future<Void> rf = strategy.scheduleReconnect();
				rf.addListener(new FutureListener<Void>() {
					@Override
					public void operationComplete(final Future<Void> future) {
						Preconditions.checkState(pending == future);

						if (future.isSuccess())
							connect();
						else
							promise.setFailure(future.cause());
					}
				});

				pending = rf;
			}
		}

		synchronized void connect() {
			try {
				final int timeout = strategy.getConnectTimeout();
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
				pending = b.connect(connection.getPeerAddress()).addListener(this);
			} catch (Exception e) {
				promise.setFailure(e);
			}
		}

		private synchronized boolean cancelConnection(final boolean mayInterruptIfRunning) {
			return pending.cancel(mayInterruptIfRunning);
		}
	}

	@Override
	public <T extends ProtocolSession> Future<T> createClient(final ProtocolConnection connection, final ProtocolSessionFactory<T> sfactory, final ReconnectStrategy strategy) {
		final ConnectionRetryListener<T> l = new ConnectionRetryListener<>(connection, sfactory, strategy);

		l.connect();

		logger.debug("Client created.");
		return l.promise;
	}

	@Override
	public void close() throws IOException {
		this.workerGroup.shutdownGracefully();
		this.bossGroup.shutdownGracefully();
	}

	@Override
	public void onSessionClosed(final ProtocolSession session) {
		synchronized (this.clientSessions) {
			logger.trace("Removing client session: {}", session);
			final Channel ch = this.clientSessions.get(session);
			ch.close();
			this.clientSessions.remove(session);
			logger.debug("Removed client session: {}", session.toString());
		}
	}

	void onServerClosed(final ProtocolServer server) {
		synchronized (this.serverSessions) {
			logger.trace("Removing server session: {}", server);
			final Channel ch = this.serverSessions.get(server);
			ch.close();
			this.clientSessions.remove(server);
			logger.debug("Removed server session: {}", server.toString());
		}
	}
}
