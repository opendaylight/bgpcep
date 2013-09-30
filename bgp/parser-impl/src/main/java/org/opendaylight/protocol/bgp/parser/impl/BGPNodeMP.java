/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.ProtocolId;

/**
 * 
 * Link State MP_(UN)REACH_NLRI (contains BGP Node inf.)
 * 
 */
public class BGPNodeMP extends AbstractLinkstateMP<NodeIdentifier> {

	private final Set<NodeIdentifier> nodes;

	/**
	 * Creates BGP Node MP Reach.
	 * 
	 * @param identifier long
	 * @param sourceProtocol {@link ProtocolId}
	 * @param prefixes set of prefix descriptors
	 * @param reachable true if the attribute is MPReach, false if the attribute is MPUnreach
	 */
	public BGPNodeMP(final long identifier, final ProtocolId sourceProtocol, final boolean reachable, final Set<NodeIdentifier> nodes) {
		super(identifier, sourceProtocol, reachable);
		this.nodes = nodes;
	}

	@Override
	public Set<NodeIdentifier> getNlri() {
		return this.nodes;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.nodes == null) ? 0 : this.nodes.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BGPNodeMP other = (BGPNodeMP) obj;
		if (this.nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!this.nodes.equals(other.nodes))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPNodeMP [nodes=");
		builder.append(this.nodes);
		builder.append("]");
		return builder.toString();
	}
}
