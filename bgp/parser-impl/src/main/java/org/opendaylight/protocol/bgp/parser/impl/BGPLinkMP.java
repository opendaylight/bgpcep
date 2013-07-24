/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.SourceProtocol;

/**
 * 
 * Link State MP_(UN)REACH_NLRI (contains BGP Link inf.)
 * 
 */
public class BGPLinkMP extends AbstractLinkstateMP<LinkIdentifier> {

	private final Set<LinkIdentifier> link;

	/**
	 * Creates BGP LINK MP Reach.
	 * 
	 * @param identifier long
	 * @param sourceProtocol {@link SourceProtocol}
	 * @param prefixes set of prefix descriptors
	 * @param reachable true if the attribute is MPReach, false if the attribute is MPUnreach
	 */
	public BGPLinkMP(final long identifier, final SourceProtocol sourceProtocol, final boolean reachable, final Set<LinkIdentifier> link) {
		super(identifier, sourceProtocol, reachable);
		this.link = link;
	}

	@Override
	public Set<LinkIdentifier> getNlri() {
		return this.link;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.link == null) ? 0 : this.link.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BGPLinkMP other = (BGPLinkMP) obj;
		if (this.link == null) {
			if (other.link != null)
				return false;
		} else if (!this.link.equals(other.link))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPLinkMP [link=");
		builder.append(this.link);
		builder.append("]");
		return builder.toString();
	}
}
