/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHop;

/**
 * 
 * MP_(UN)REACH_NLRI (basic, IPv4)
 * 
 */
public class IPv4MP implements MPReach<Prefix<IPv4Address>> {

	private final boolean reachable;

	private final CIpv4NextHop nextHop;

	private final Set<Prefix<IPv4Address>> nlri;

	public IPv4MP(final boolean reachable, final CIpv4NextHop nextHop, final Set<Prefix<IPv4Address>> nlri) {
		this.reachable = reachable;
		this.nextHop = nextHop;
		this.nlri = nlri;
	}

	@Override
	public boolean isReachable() {
		return this.reachable;
	}

	@Override
	public Set<Prefix<IPv4Address>> getNlri() {
		return this.nlri;
	}

	public CIpv4NextHop getNextHop() {
		return this.nextHop;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.nextHop == null) ? 0 : this.nextHop.hashCode());
		result = prime * result + ((this.nlri == null) ? 0 : this.nlri.hashCode());
		result = prime * result + (this.reachable ? 1231 : 1237);
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
		if (!(obj instanceof IPv4MP))
			return false;
		final IPv4MP other = (IPv4MP) obj;
		if (this.nextHop == null) {
			if (other.nextHop != null)
				return false;
		} else if (!this.nextHop.equals(other.nextHop))
			return false;
		if (this.nlri == null) {
			if (other.nlri != null)
				return false;
		} else if (!this.nlri.equals(other.nlri))
			return false;
		if (this.reachable != other.reachable)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("IPv4MP [reachable=");
		builder.append(this.reachable);
		builder.append(", nextHop=");
		builder.append(this.nextHop);
		builder.append(", nlri=");
		builder.append(this.nlri);
		builder.append("]");
		return builder.toString();
	}
}
