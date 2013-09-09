/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public class PCEPDispatcherImpl extends AbstractDispatcher implements PCEPDispatcher {
	private static final PCEPMessageFactory msgFactory = new PCEPMessageFactory();
	private final SessionNegotiatorFactory<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> snf;

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 * 
	 * @throws IOException if some error occurred during opening the selector
	 */
	public PCEPDispatcherImpl(final SessionNegotiatorFactory<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> snf) {
		super();
		this.snf = snf;
	}

	@Override
	public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<PCEPSessionListener> listenerFactory) {
		return this.createServer(address, listenerFactory, snf, msgFactory);
	}

	/**
	 * Create client is used for mock purposes only.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Override
	public Future<? extends PCEPSession> createClient(final InetSocketAddress address, final PCEPSessionListener listener, final ReconnectStrategy strategy) {
		return this.createClient(address, listener, snf, msgFactory, strategy);
	}
}
