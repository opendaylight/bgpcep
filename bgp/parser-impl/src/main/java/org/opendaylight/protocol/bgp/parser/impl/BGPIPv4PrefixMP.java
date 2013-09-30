/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.IPv4PrefixIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;

/**
 * IPv4 implementation of {@link MPReach}.
 */
public class BGPIPv4PrefixMP extends AbstractLinkstateMP<IPv4PrefixIdentifier> {

	private final Set<IPv4PrefixIdentifier> prefixes;

	/**
	 * Creates BGP IPv4 Prefix.
	 * 
	 * @param identifier long
	 * @param sourceProtocol {@link ProtocolId}
	 * @param prefixes set of prefixes
	 * @param reachable true if the attribute is MPReach, false if the attribute is MPUnreach
	 */
	public BGPIPv4PrefixMP(final long identifier, final ProtocolId sourceProtocol, final Set<IPv4PrefixIdentifier> prefixes,
			final boolean reachable) {
		super(identifier, sourceProtocol, reachable);
		this.prefixes = prefixes;
	}

	@Override
	public Set<IPv4PrefixIdentifier> getNlri() {
		return this.prefixes;
	}
}
