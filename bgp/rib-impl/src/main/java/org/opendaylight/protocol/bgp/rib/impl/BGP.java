/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

/**
 * BGP interface. At this time it only supports listening to changes seen by the backing device, typically a network
 * element. Abstracts away connection issues - listener starts getting notifications once connection is established.
 * Implementation of this interface is required to send all previous messages.
 */
public interface BGP {
	/**
	 * Register for BGP update feed. Specified listener will have the BGP information synchronized. The registration
	 * needs to be explicitly closed in order to stop receiving the updates.
	 * 
	 * @param listener {@link BGPSessionListener}
	 * @param tcpStrategyFactory {@link ReconnectStrategyFactory} to use for creating TCP-level retry strategies
	 * @param sessionStrategy {@link ReconnectStrategy} to use for session-level retries
	 * @throws IllegalStateException if there is already a listener registered
	 * @return ListenerRegistration
	 */
	ListenerRegistration<BGPSessionListener> registerUpdateListener(BGPSessionListener listener,
			ReconnectStrategyFactory tcpStrategyFactory, ReconnectStrategy sessionStrategy);
}
