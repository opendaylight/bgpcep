/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.base.Preconditions;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl extends AbstractDispatcher<BGPSessionImpl, BGPSessionListener> implements BGPDispatcher, AutoCloseable {
	private final BGPHandlerFactory hf;
	private final Timer timer;

	public BGPDispatcherImpl(final MessageRegistry messageRegistry, final Timer timer, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
		super(bossGroup, workerGroup);
		this.timer = Preconditions.checkNotNull(timer);
		this.hf = new BGPHandlerFactory(messageRegistry);
	}

	@Override
	public Future<BGPSessionImpl> createClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
			final AsNumber remoteAs, final BGPSessionListener listener, final ReconnectStrategy strategy) {
		final BGPSessionNegotiatorFactory snf = new BGPSessionNegotiatorFactory(this.timer, preferences, remoteAs);
		final SessionListenerFactory<BGPSessionListener> slf = new SessionListenerFactory<BGPSessionListener>() {
			@Override
			public BGPSessionListener getSessionListener() {
				return listener;
			}
		};
		return super.createClient(address, strategy, new PipelineInitializer<BGPSessionImpl>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
				ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
				ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(slf, ch, promise));
				ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
			}
		});
	}

	@Override
	public Future<Void> createReconnectingClient(final InetSocketAddress address,
			final BGPSessionPreferences preferences, final AsNumber remoteAs,
			final BGPSessionListener listener, final ReconnectStrategyFactory connectStrategyFactory,
			final ReconnectStrategyFactory reestablishStrategyFactory) {
		final BGPSessionNegotiatorFactory snf = new BGPSessionNegotiatorFactory(this.timer, preferences, remoteAs);
		final SessionListenerFactory<BGPSessionListener> slf = new SessionListenerFactory<BGPSessionListener>() {
			@Override
			public BGPSessionListener getSessionListener() {
				return listener;
			}
		};

		return super.createReconnectingClient(address, connectStrategyFactory, reestablishStrategyFactory.createReconnectStrategy(), new PipelineInitializer<BGPSessionImpl>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
				ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
				ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(slf, ch, promise));
				ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
			}
		});
	}

	@Override
	public void close() {
	}
}
