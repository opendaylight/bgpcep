/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.impl.util.DeserializerExceptionHandler;
import org.opendaylight.netconf.util.ChannelInitializer;
import org.opendaylight.protocol.framework.AbstractDispatcher;

import com.google.common.base.Optional;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

public class NetconfServerDispatcher extends AbstractDispatcher<NetconfSession, NetconfServerSessionListener> {

	private final ServerChannelInitializer initializer;

	public NetconfServerDispatcher(final Optional<SSLContext> maybeContext, NetconfServerSessionNegotiatorFactory serverNegotiatorFactory,
			NetconfServerSessionListenerFactory listenerFactory) {
		this.initializer = new ServerChannelInitializer(maybeContext, serverNegotiatorFactory, listenerFactory);
	}

	// FIXME change headers for all new source code files

	// TODO test create server with same address twice
	public ChannelFuture createServer(InetSocketAddress address) {

		return super.createServer(address, new PipelineInitializer<NetconfSession>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<NetconfSession> promise) {
				initializer.initialize(ch, promise);
			}
		});
	}

	private static class ServerChannelInitializer extends ChannelInitializer {

		private final NetconfServerSessionNegotiatorFactory negotiatorFactory;
		private final NetconfServerSessionListenerFactory listenerFactory;

		private ServerChannelInitializer(Optional<SSLContext> maybeContext, NetconfServerSessionNegotiatorFactory negotiatorFactory,
				NetconfServerSessionListenerFactory listenerFactory) {
			super(maybeContext);
			this.negotiatorFactory = negotiatorFactory;
			this.listenerFactory = listenerFactory;
		}

		@Override
		protected void initializeAfterDecoder(SocketChannel ch, Promise<? extends NetconfSession> promise) {
			ch.pipeline().addLast("deserializerExHandler", new DeserializerExceptionHandler());
			ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
		}

		@Override
		protected void initSslEngine(SSLEngine sslEngine) {
			sslEngine.setUseClientMode(false);
		}
	}

}
