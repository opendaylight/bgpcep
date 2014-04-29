/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.protocol.framework.SessionListenerFactory;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface PCEPDispatcher {
	/**
	 * Creates server. Each server needs three factories to pass their instances to client sessions.
	 *
	 * @param address to be bound with the server
	 * @param listenerFactory to create listeners for clients
	 * @return instance of PCEPServer
	 */
	ChannelFuture createServer(InetSocketAddress address, SessionListenerFactory<PCEPSessionListener> listenerFactory);

	/**
	 * Creates server. Each server needs three factories to pass their instances to client sessions.
	 *
	 * @param address to be bound with the server
	 * @param keys RFC2385 key mapping
	 * @param listenerFactory to create listeners for clients
	 * @return instance of PCEPServer
	 */
	ChannelFuture createServer(InetSocketAddress address, KeyMapping keys, SessionListenerFactory<PCEPSessionListener> listenerFactory);
}
