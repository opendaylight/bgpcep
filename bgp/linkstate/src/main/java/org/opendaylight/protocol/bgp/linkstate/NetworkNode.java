/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;

/**
 * A single (router) node in the network topology. Nodes are interconnected by links and have a bunch of attributes. One
 * of the key attributes is the set of prefixes for which this node acts as a network edge router.
 */
public interface NetworkNode extends NetworkObject<NodeIdentifier> {
	@Override
	public NetworkNodeState currentState();
}
