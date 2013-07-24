/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;

/**
 *
 * A single link in network topology. Network link is a connecting line between
 * two network nodes with bunch of attributes.
 */
public interface NetworkLink extends NetworkObject<LinkIdentifier> {
	@Override
	public NetworkLinkState currentState();
}
