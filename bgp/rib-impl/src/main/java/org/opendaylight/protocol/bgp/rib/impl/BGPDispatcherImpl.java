/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListenerFactory;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl extends AbstractDispatcher<BGPSessionImpl> implements BGPDispatcher {
	private final Timer timer = new HashedWheelTimer();

	private BGPSessionNegotiatorFactory snf;

	private final BGPHandlerFactory hf;

	private SessionListenerFactory<BGPSessionListener> slf;

	public BGPDispatcherImpl(final BGPMessageFactory parser) {
		super();
		this.hf = new BGPHandlerFactory(parser);
	}

	@Override
	public Future<? extends BGPSession> createClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
			final BGPSessionListener listener, final ReconnectStrategy strategy) {
		this.snf = new BGPSessionNegotiatorFactory(this.timer, preferences);
		this.slf = new SessionListenerFactory<BGPSessionListener>() {

			@Override
			public BGPSessionListener getSessionListener() {
				return listener;
			}
		};
		return createClient(address, strategy);
	}

	public ChannelFuture createMockServer(final InetSocketAddress address) {
		return this.createServer(address);
	}

	@Override
	public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
		ch.pipeline().addLast(this.hf.getDecoders());
		ch.pipeline().addLast("negotiator", this.snf.getSessionNegotiator(this.slf, ch, promise));
		ch.pipeline().addLast(this.hf.getEncoders());
	}
}
