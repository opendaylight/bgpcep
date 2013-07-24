/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.io.IOException;

import org.opendaylight.protocol.bgp.parser.BGPMessageParser;
import org.opendaylight.protocol.bgp.parser.BGPSession;


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
	BGPSession createClient(BGPConnection connection, BGPMessageParser parser) throws IOException;
}
