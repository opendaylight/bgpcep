/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Object representation of basic Extended Community, as defined by RFC4360. Extended communities are semantic tags,
 * pretty much same way communities defined in RFC1997, with the notable exceptions of supporting four-byte AS numbers
 * and having a more complicated structure.
 */
public abstract class ExtendedCommunity implements Serializable {

	private static final long serialVersionUID = -6279624143516991855L;
	private final boolean ianaAuthority;
	private final boolean transitive;

	/**
	 * Initialize the base class with specified authority/transitive bits.
	 * 
	 * @param ianaAuthority Value of the 'IANA Authority' bit
	 * @param transitive Value of the 'Transitive' bit
	 */
	protected ExtendedCommunity(final boolean ianaAuthority, final boolean transitive) {
		this.ianaAuthority = ianaAuthority;
		this.transitive = transitive;
	}

	/**
	 * Check what authority IANA holds over this extended community.
	 * 
	 * @return <li>false IANA-assignable type using the "First Come First Serve" policy
	 * 
	 *         <li>true Part of this Type Field space is for IANA assignable types using either the Standard Action or
	 *         the Early IANA Allocation policy. The rest of this Type Field space is for Experimental use.
	 */
	public final boolean getIanaAuthority() {
		return this.ianaAuthority;
	}

	/**
	 * Check if a community is transitive.
	 * 
	 * @return <li>true: the community is transitive <li>false: the community is not transitive
	 */
	public final boolean isTransitive() {
		return this.transitive;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.ianaAuthority ? 1231 : 1237);
		result = prime * result + (this.transitive ? 1231 : 1237);
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
		if (!(obj instanceof ExtendedCommunity))
			return false;
		final ExtendedCommunity other = (ExtendedCommunity) obj;
		if (this.ianaAuthority != other.ianaAuthority)
			return false;
		if (this.transitive != other.transitive)
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("ianaAuthority", ianaAuthority);
		toStringHelper.add("transitive", transitive);
		return toStringHelper;
	}
}
