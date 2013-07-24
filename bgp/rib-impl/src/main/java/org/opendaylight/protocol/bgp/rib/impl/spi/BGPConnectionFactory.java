/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.ProtocolConnectionFactory;

/**
 * BGP implementation of {@link ProtocolConnectionFactory}
 */
public interface BGPConnectionFactory extends ProtocolConnectionFactory {
	@Override
	BGPConnection createProtocolConnection(final InetSocketAddress address);
}
