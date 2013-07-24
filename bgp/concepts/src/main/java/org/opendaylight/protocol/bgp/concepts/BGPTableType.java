/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.Identifier;
import com.google.common.base.Preconditions;

/**
 * Utility class identifying a BGP table type. A table type is formed by two identifiers: AFI and SAFI.
 */
public final class BGPTableType implements Comparable<BGPTableType>, Identifier {

	private static final long serialVersionUID = -5502662876916458740L;

	private final BGPSubsequentAddressFamily safi;

	private final BGPAddressFamily afi;

	/**
	 * Creates BGP Table type.
	 * 
	 * @param afi Address Family Identifier
	 * @param safi Subsequent Address Family Identifier
	 */
	public BGPTableType(final BGPAddressFamily afi, final BGPSubsequentAddressFamily safi) {
		this.afi = Preconditions.checkNotNull(afi, "Address family may not be null");
		this.safi = Preconditions.checkNotNull(safi, "Subsequent address family may not be null");
	}

	/**
	 * Returns Address Family Identifier.
	 * 
	 * @return afi AFI
	 */
	public BGPAddressFamily getAddressFamily() {
		return this.afi;
	}

	/**
	 * Returns Subsequent Address Family Identifier.
	 * 
	 * @return safi SAFI
	 */
	public BGPSubsequentAddressFamily getSubsequentAddressFamily() {
		return this.safi;
	}

	@Override
	public int hashCode() {
		int ret = 3 * this.afi.hashCode();
		ret += this.safi.hashCode();
		return ret;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj != null && obj instanceof BGPTableType) {
			final BGPTableType o = (BGPTableType) obj;
			return this.afi.equals(o.afi) && this.safi.equals(o.safi);
		}
		return false;
	}

	@Override
	public int compareTo(final BGPTableType other) {
		if (other == null)
			return 1;

		final int c = this.afi.compareTo(other.afi);
		if (c != 0)
			return c;
		return this.safi.compareTo(other.safi);
	}

	@Override
	public String toString() {
		return this.afi.toString() + "." + this.safi.toString();
	}
}
