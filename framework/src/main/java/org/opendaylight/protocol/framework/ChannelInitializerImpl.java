/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class ChannelInitializerImpl<M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends ChannelInitializer<SocketChannel> {
	private static final Logger logger = LoggerFactory.getLogger(ChannelInitializerImpl.class);
	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final SessionListenerFactory<L> listenerFactory;
	private final ProtocolHandlerFactory<?> factory;
	private final Promise<S> promise;

	ChannelInitializerImpl(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final SessionListenerFactory<L> listenerFactory,
			final ProtocolHandlerFactory<?> factory, final Promise<S> promise) {
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.listenerFactory = Preconditions.checkNotNull(listenerFactory);
		this.promise = Preconditions.checkNotNull(promise);
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	protected void initChannel(final SocketChannel ch) {
		logger.debug("Initializing channel {}", ch);
		ch.pipeline().addLast("decoder", factory.getDecoder());
		ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
		ch.pipeline().addLast("encoder", factory.getEncoder());
		logger.debug("Channel {} initialized", ch);
	}
}