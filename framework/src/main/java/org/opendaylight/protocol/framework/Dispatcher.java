/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * Dispatcher class for creating protocol servers and clients.
 */
public interface Dispatcher {
	/**
	 * Creates server. Each server needs factories to pass their instances to client sessions.
	 * 
	 * @param connectionFactory factory for connection specific attributes
	 * @param sfactory to create specific session
	 * @param handlerFactory protocol specific channel handlers factory
	 * 
	 * @return instance of ProtocolServer
	 */
	public Future<ProtocolServer> createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory<?> sfactory);

	/**
	 * Creates a client.
	 * 
	 * @param connection connection specific attributes
	 * @param sfactory protocol session factory to create a specific session
	 * @param strategy Reconnection strategy to be used when initial connection fails
	 * 
	 * @return session associated with this client
	 */
	public <T extends ProtocolSession> Future<T> createClient(final ProtocolConnection connection,
			final ProtocolSessionFactory<T> sfactory, final ReconnectStrategy strategy);
}
