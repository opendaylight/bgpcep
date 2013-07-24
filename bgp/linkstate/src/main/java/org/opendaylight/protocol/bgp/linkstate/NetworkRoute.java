/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.concepts.Prefix;

/**
 * A single route existing within the network. A route is a way how to get from
 * the local node to a set of network addresses. The set of addresses is
 * represented as a Prefix of a particular address type and is the unique
 * identifier for the route. The routing part is represented as the
 * directly-connected neighbor, which should be used used as a relay for traffic
 * going to the set of addresses.
 *
 * @param <T>
 */
public interface NetworkRoute<T extends NetworkAddress<?>> extends NetworkObject<Prefix<T>> {
	@Override
	public NetworkRouteState<T> currentState();
}

