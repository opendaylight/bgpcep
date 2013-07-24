/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.net.InetSocketAddress;

/**
 * Factory creating Protocol connections.
 */
public interface ProtocolConnectionFactory {

	/**
	 * Returns new Protocol Connection object. The rest of the attributes are
	 * protocol specific.
	 * @param address to be bind
	 * @return new Protocol Connection.
	 */
	ProtocolConnection createProtocolConnection(final InetSocketAddress address);
}
