/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;
import java.util.Arrays;

import org.opendaylight.protocol.concepts.IPv4Address;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing a <a href="http://tools.ietf.org/html/rfc4360#section-3.2">IPv4 Address Specific Extended
 * Community</a>.
 */
public class Inet4SpecificExtendedCommunity extends ExtendedCommunity implements Serializable {

	private static final long serialVersionUID = 1355924956883963072L;
	private final IPv4Address globalAdmin;
	private final byte[] localAdmin;
	private final int subType;

	/**
	 * Create a new IPv4 address-specific extended community.
	 * 
	 * @param transitive True if the community is transitive
	 * @param subType Community subtype, has to be in range 0-255
	 * @param globalAdmin Globally-assigned namespace (IPv4 address)
	 * @param localAdmin byte[] Locally-assigned value, has to be 2 bytes long
	 * @throws IllegalArgumentException when either subtype or localAdmin is invalid
	 */
	public Inet4SpecificExtendedCommunity(final boolean transitive, final int subType, final IPv4Address globalAdmin,
			final byte[] localAdmin) {
		super(false, transitive);
		Preconditions.checkArgument(subType > 0 && subType < 255, "Invalid Sub-Type " + subType);
		Preconditions.checkArgument(localAdmin.length == 2, "Invalid Local Administrator");
		this.subType = subType;
		this.globalAdmin = Preconditions.checkNotNull(globalAdmin);
		this.localAdmin = localAdmin;
	}

	/**
	 * Returns the community subtype.
	 * 
	 * @return Community subtype
	 */
	public final int getSubType() {
		return this.subType;
	}

	/**
	 * Returns the globally-assigned namespace.
	 * 
	 * @return Globally-assigned namespace
	 */
	public final IPv4Address getGlobalAdmin() {
		return this.globalAdmin;
	}

	/**
	 * Returns the locally-assigned community value.
	 * 
	 * @return Locally-assigned community value
	 */
	public final byte[] getLocalAdmin() {
		return this.localAdmin;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("subType", this.subType);
		toStringHelper.add("globalAdmin", this.globalAdmin);
		toStringHelper.add("localAdmin", this.localAdmin);
		return super.addToStringAttributes(toStringHelper);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.globalAdmin == null) ? 0 : this.globalAdmin.hashCode());
		result = prime * result + Arrays.hashCode(this.localAdmin);
		result = prime * result + this.subType;
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
		if (getClass() != obj.getClass())
			return false;
		final Inet4SpecificExtendedCommunity other = (Inet4SpecificExtendedCommunity) obj;
		if (this.globalAdmin == null) {
			if (other.globalAdmin != null)
				return false;
		} else if (!this.globalAdmin.equals(other.globalAdmin))
			return false;
		if (!Arrays.equals(this.localAdmin, other.localAdmin))
			return false;
		if (this.subType != other.subType)
			return false;
		return true;
	}
}
