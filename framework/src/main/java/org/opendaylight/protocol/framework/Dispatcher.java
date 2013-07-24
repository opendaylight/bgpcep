/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

/**
 * Dispatcher class for creating protocol servers and clients. The idea is to first create servers and clients and the run the start method
 * that will handle sockets in different thread.
 */
public interface Dispatcher {
	/**
	 * Creates server. Each server needs factories to pass their instances to client sessions.
	 * @param address to be bound with the server
	 * @param connectionFactory factory for connection specific attributes
	 * @param sfactory to create specific session
	 * @param isFactory protocol specific input stream factory
	 *
	 * @return instance of ProtocolServer
	 * @throws IOException if some IO error occurred
	 */
	public ProtocolServer createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sfactory, final ProtocolInputStreamFactory isFactory) throws IOException;

	/**
	 * Creates secure server. Each server needs factories to pass their instances to client sessions.
	 * @param address to be bound with the server
	 * @param connectionFactory factory for connection specific attributes
	 * @param sfactory to create specific session
	 * @param isFactory protocol-specific input stream factory
	 * @param context SSLContext to use in secure communications
	 *
	 * @return instance of ProtocolServer
	 * @throws IOException if some IO error occurred
	 */
	public ProtocolServer createServer(final InetSocketAddress address, final ProtocolConnectionFactory connectionFactory,
			final ProtocolSessionFactory sfactory, final ProtocolInputStreamFactory isFactory, final SSLContext context) throws IOException;

	/**
	 * Creates a client.
	 * @param connection connection specific attributes
	 * @param sfactory protocol message factory to be passed to session
	 * @param isFactory protocol-specific input stream factory
	 *
	 * @return session associated with this client
	 * @throws IOException if some IO error occurred
	 */
	public ProtocolSession createClient(final ProtocolConnection connection, final ProtocolSessionFactory sfactory,
			final ProtocolInputStreamFactory isFactory) throws IOException;

	/**
	 * Creates secure client.
	 * @param connection connection specific attributes
	 * @param sfactory protocol message factory to be passed to session
	 * @param isFactory protocol-specific input stream factory
	 * @param context SSLContext to use in secure communications
	 *
	 * @return session associated with this client
	 * @throws IOException if some IO error occurred
	 */
	public ProtocolSession createClient(final ProtocolConnection connection, final ProtocolSessionFactory sfactory,
			final ProtocolInputStreamFactory isFactory, final SSLContext context) throws IOException;
}
