/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.io.Serializable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

import com.google.common.base.Preconditions;

/**
 * Object representation of a RFC1997 Community tag. Communities are a way for the advertising entitiy to attach
 * semantic meaning/policy to advertised objects.
 */
public final class CommunityImpl implements Community, Serializable {
	/**
	 * NO_EXPORT community. All routes received carrying a communities attribute containing this value MUST NOT be
	 * advertised outside a BGP confederation boundary (a stand-alone autonomous system that is not part of a
	 * confederation should be considered a confederation itself).
	 */
	public static final CommunityImpl NO_EXPORT = new CommunityImpl(new AsNumber((long) 0xFFFF), 0xFF01);
	/**
	 * NO_ADVERTISE community. All routes received carrying a communities attribute containing this value MUST NOT be
	 * advertised to other BGP peers.
	 */
	public static final CommunityImpl NO_ADVERTISE = new CommunityImpl(new AsNumber((long) 0xFFFF), 0xFF02);
	/**
	 * NO_EXPORT_SUBCONFED community. All routes received carrying a communities attribute containing this value MUST
	 * NOT be advertised to external BGP peers (this includes peers in other members autonomous systems inside a BGP
	 * confederation).
	 */
	public static final CommunityImpl NO_EXPORT_SUBCONFED = new CommunityImpl(new AsNumber((long) 0xFFFF), 0xFF03);

	private static final long serialVersionUID = -944853598551415685L;

	private final int semantics;

	private final AsNumber as;

	/**
	 * Create a new community tag for a particular AS number and semantics.
	 * 
	 * @param as Global semantics namespace identifier (usually the tagging Autonomous System)
	 * @param semantics Sematics identifier (specific meaning defined externally by the namespace)
	 */
	public CommunityImpl(final AsNumber as, final int semantics) {
		Preconditions.checkArgument(semantics > 0 && semantics < 65535, "Invalid semantics specified");
		this.semantics = semantics;
		this.as = as;
	}

	/**
	 * Return semantics attribute of community.
	 * 
	 * @return Semantics attribute
	 */
	@Override
	public Long getSemantics() {
		return (long) this.semantics;
	}

	/**
	 * Return ASNumber of community.
	 * 
	 * @return {@link AsNumber}
	 */
	@Override
	public AsNumber getAsNumber() {
		return this.as;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof CommunityImpl))
			return false;

		final CommunityImpl c = (CommunityImpl) o;
		return this.as.equals(c.as) && this.semantics == c.semantics;
	}

	@Override
	public int hashCode() {
		return 7 * this.as.hashCode() + 13 * this.semantics;
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder(this.as.toString());
		sb.append(':');
		sb.append(this.semantics);
		return sb.toString();
	}

	/**
	 * Creates a Community from its String representation.
	 * 
	 * @param string String representation of a community
	 * @return new Community
	 */
	public static CommunityImpl valueOf(final String string) {
		final String[] parts = string.split(":", 2);

		final int asn = Integer.valueOf(parts[0]);
		final int sem = Integer.valueOf(parts[1]);
		return new CommunityImpl(new AsNumber((long) asn), sem);
	}
}
