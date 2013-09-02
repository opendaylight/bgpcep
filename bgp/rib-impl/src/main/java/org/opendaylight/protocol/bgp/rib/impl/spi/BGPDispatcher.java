/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.framework.ReconnectStrategy;

/**
 * Dispatcher class for creating BGP clients.
 */
public interface BGPDispatcher {

	/**
	 * Creates BGP client.
	 * 
	 * @param connection attributes required for connection
	 * @param parser BGP message parser
	 * @return client session
	 * @throws IOException
	 */
	Future<? extends BGPSession> createClient(InetSocketAddress address, BGPSessionPreferences preferences, BGPSessionListener listener, final ReconnectStrategy strategy);
}
