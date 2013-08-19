/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.ProtocolServer;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface PCEPDispatcher {

	/**
	 * Creates server. Each server needs three factories to pass their instances to client sessions.
	 * @param address to be bound with the server
	 * @param listenerFactory to create listeners for clients
	 * @param proposalFactory to create proposed open objects for clients
	 * @param checkerFactory to create session characteristics checker for clients
	 * @return instance of PCEPServer
	 * @throws IOException if some IO error occurred
	 */
	public Future<ProtocolServer> createServer(final InetSocketAddress address, final PCEPConnectionFactory connectionFactory) throws IOException;

	/**
	 * Creates a client. Needs to be started via the start method.
	 * @param address of the server, where the client will connect
	 * @param sessionListener listener to this session who will be notified about incoming messages and other session events
	 * @param proposal proposes initial Open object
	 * @param checker checks session characteristics and proposes another Open object if necessary
	 * @return session associated with this client.
	 * @throws IOException if some IO error occurred
	 */
	public Future<? extends PCEPSession> createClient(PCEPConnection connection) throws IOException;

	/**
	 * Sets the limit of maximum unknown messages per minute. If not set by the user, default is 5 messages/minute.
	 * @param limit maximum unknown messages per minute
	 */
	public void setMaxUnknownMessages(final int limit);
}

