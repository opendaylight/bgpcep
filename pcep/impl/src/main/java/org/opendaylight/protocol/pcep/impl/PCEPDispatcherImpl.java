/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl extends AbstractDispatcher<PCEPSessionImpl> implements PCEPDispatcher {

	private final SessionNegotiatorFactory<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> snf;

	private SessionListenerFactory<PCEPSessionListener> slf;

	private final PCEPHandlerFactory hf = new PCEPHandlerFactory();

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 * 
	 * @throws IOException if some error occurred during opening the selector
	 */
	public PCEPDispatcherImpl(final SessionNegotiatorFactory<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory) {
		super();
		this.snf = negotiatorFactory;
	}

	@Override
	public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<PCEPSessionListener> listenerFactory) {
		this.slf = listenerFactory;
		return this.createServer(address);
	}

	/**
	 * Create client is used for mock purposes only.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public Future<? extends PCEPSession> createClient(final InetSocketAddress address, final PCEPSessionListener listener,
			final ReconnectStrategy strategy) {
		this.slf = new SessionListenerFactory<PCEPSessionListener>() {

			@Override
			public PCEPSessionListener getSessionListener() {
				return listener;
			}

		};
		return this.createClient(address, strategy);
	}

	@Override
	public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
		ch.pipeline().addLast(this.hf.getDecoders());
		ch.pipeline().addLast("negotiator", this.snf.getSessionNegotiator(this.slf, ch, promise));
		ch.pipeline().addLast(this.hf.getEncoders());
		System.out.println("Pipeline " + Arrays.toString(ch.pipeline().names().toArray()));
	}
}
