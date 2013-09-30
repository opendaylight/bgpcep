/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.IPv6PrefixIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;

/**
 * IPv6 implementation of {@link MPReach}.
 */
public class BGPIPv6PrefixMP extends AbstractLinkstateMP<IPv6PrefixIdentifier> {

	private final Set<IPv6PrefixIdentifier> prefixes;

	/**
	 * Creates BGP IPv6 Prefix.
	 * 
	 * @param identifier long
	 * @param sourceProtocol {@link ProtocolId}
	 * @param prefixes set of prefix descriptors
	 * @param reachable true if the attribute is MPReach, false if the attribute is MPUnreach
	 */
	public BGPIPv6PrefixMP(final long identifier, final ProtocolId sourceProtocol, final Set<IPv6PrefixIdentifier> prefixes,
			final boolean reachable) {
		super(identifier, sourceProtocol, reachable);
		this.prefixes = prefixes;
	}

	@Override
	public Set<IPv6PrefixIdentifier> getNlri() {
		return this.prefixes;
	}
}
