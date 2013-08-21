/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.opendaylight.protocol.framework.Dispatcher;
import org.opendaylight.protocol.framework.ProtocolServer;
import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPConnectionFactory;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl implements PCEPDispatcher {

	public static final int DEFAULT_MAX_UNKNOWN_MSG = 5;

	private int maxUnknownMessages = DEFAULT_MAX_UNKNOWN_MSG;

	private final Dispatcher dispatcher;

	private final PCEPSessionProposalFactory proposalFactory;

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 * 
	 * @throws IOException if some error occurred during opening the selector
	 */
	public PCEPDispatcherImpl(final Dispatcher dispatcher, final PCEPSessionProposalFactory proposalFactory) {
		this.dispatcher = dispatcher;
		this.proposalFactory = proposalFactory;
	}

	@Override
	public Future<ProtocolServer> createServer(final InetSocketAddress address, final PCEPConnectionFactory connectionFactory) throws IOException {
		connectionFactory.setProposal(this.proposalFactory, address, 0);
		return this.dispatcher.createServer(address, connectionFactory, new PCEPSessionFactoryImpl(this.maxUnknownMessages));
	}

	/**
	 * Create client is used for mock purposes only.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@Override
	public Future<? extends PCEPSession> createClient(final PCEPConnection connection) throws IOException {
		return this.dispatcher.createClient(connection, new PCEPSessionFactoryImpl(this.maxUnknownMessages));
	}

	@Override
	public void setMaxUnknownMessages(final int limit) {
		this.maxUnknownMessages = limit;
	}

	public int getMaxUnknownMessages() {
		return this.maxUnknownMessages;
	}

	public Dispatcher getDispatcher() {
		return this.dispatcher;
	}
}
