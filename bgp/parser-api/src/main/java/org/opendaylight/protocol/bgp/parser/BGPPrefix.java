/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BGPObject;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;

/**
 * @see <a href="http://tools.ietf.org/html/draft-ietf-idr-ls-distribution-02#section-3.2">Figure 9: The IPv4/IPv6
 *      Topology Prefix NLRI format</a>
 * 
 * @param <T> Network Address type of the prefix
 */
public interface BGPPrefix<T extends NetworkAddress<T>> extends BGPObject {
	@Override
	public BGPPrefixState currentState();

	/**
	 * Return the Prefix Identifier.
	 * 
	 * @return Prefix Identifier
	 */
	public PrefixIdentifier<T> getPrefixIdentifier();
}
