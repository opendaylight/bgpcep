/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Preconditions;

@ThreadSafe
final class ProtocolSessionPromise<M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends DefaultPromise<S> {
	private final ChannelInitializerImpl<M, S, L> init;
	private final ReconnectStrategy strategy;
	private final InetSocketAddress address;
	private final Bootstrap b;

	@GuardedBy("this")
	private Future<?> pending;

	ProtocolSessionPromise(final EventLoopGroup workerGroup, final InetSocketAddress address, final SessionNegotiatorFactory<M, S, L> negotiatorFactory,
			final SessionListenerFactory<L> listenerFactory,
			final ProtocolHandlerFactory<?> protocolFactory, final ReconnectStrategy strategy) {
		this.strategy = Preconditions.checkNotNull(strategy);
		this.address = Preconditions.checkNotNull(address);

		init = new ChannelInitializerImpl<M, S, L>(negotiatorFactory, listenerFactory, protocolFactory, this);
		b = new Bootstrap();
		b.group(workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).handler(init);
	}

	synchronized void connect() {
		final Object lock = this;

		try {
			final int timeout = strategy.getConnectTimeout();
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
			pending = b.connect(address).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(final ChannelFuture cf) throws Exception {
					synchronized (lock) {
						// Triggered when a connection attempt is resolved.
						Preconditions.checkState(pending == cf);

						/*
						 * The promise we gave out could have been cancelled,
						 * which cascades to the connect getting cancelled,
						 * but there is a slight race window, where the connect
						 * is already resolved, but the listener has not yet
						 * been notified -- cancellation at that point won't
						 * stop the notification arriving, so we have to close
						 * the race here.
						 */
						if (isCancelled()) {
							if (cf.isSuccess()) {
								cf.channel().close();
							}
							return;
						}

						if (!cf.isSuccess()) {
							final Future<Void> rf = strategy.scheduleReconnect(cf.cause());
							rf.addListener(new FutureListener<Void>() {
								@Override
								public void operationComplete(final Future<Void> sf) {
									synchronized (lock) {
										// Triggered when a connection attempt is to be made.
										Preconditions.checkState(pending == sf);

										/*
										 * The promise we gave out could have been cancelled,
										 * which cascades to the reconnect attempt getting
										 * cancelled, but there is a slight race window, where
										 * the reconnect attempt is already enqueued, but the
										 * listener has not yet been notified -- if cancellation
										 * happens at that point, we need to catch it here.
										 */
										if (!isCancelled()) {
											if (sf.isSuccess()) {
												connect();
											} else {
												setFailure(sf.cause());
											}
										}
									}
								}
							});

							pending = rf;
						}
					}
				}
			});
		} catch (Exception e) {
			setFailure(e);
		}
	}

	@Override
	public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
		if (super.cancel(mayInterruptIfRunning)) {
			pending.cancel(mayInterruptIfRunning);
			return true;
		}

		return false;
	}

	@Override
	public synchronized Promise<S> setSuccess(final S result) {
		strategy.reconnectSuccessful();
		return super.setSuccess(result);
	}
}