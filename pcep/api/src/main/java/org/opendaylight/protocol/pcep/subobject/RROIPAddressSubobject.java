/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

/**
 * Parametrized structure of RRO IP Address Subobject.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.4.1.1">Section 4.4.1.1: Subobject 1: IPv4 address</a> and
 *      <a href="http://tools.ietf.org/html/rfc3209#section-4.4.1.2">Section 4.4.1.2: Subobject 2: IPv6 address</a>
 * 
 * @param <T> subtype of Prefix
 */
public class RROIPAddressSubobject extends ReportedRouteSubobject {

	private final IpPrefix prefix;

	/**
	 * Local protection available
	 */
	private final boolean localProtectionAvailable;

	/**
	 * Local protection in use
	 */
	private final boolean localProtectionInUse;

	/**
	 * Constructs IPPrefix Subobject.
	 * 
	 * @param prefix T
	 * @param localProtectionAvailable boolean
	 * @param localProtectionInUse boolean
	 */
	public RROIPAddressSubobject(final IpPrefix prefix, final boolean localProtectionAvailable, final boolean localProtectionInUse) {
		super();
		this.prefix = prefix;
		this.localProtectionAvailable = localProtectionAvailable;
		this.localProtectionInUse = localProtectionInUse;
	}

	/**
	 * Gets specific {@link Prefix}.
	 * 
	 * @return prefix T
	 */
	public IpPrefix getPrefix() {
		return this.prefix;
	}

	/**
	 * Returns tru if local protection is available.
	 * 
	 * @return boolean
	 */
	public boolean isLocalProtectionAvailable() {
		return this.localProtectionAvailable;
	}

	/**
	 * Returns true if local protection is in use
	 * 
	 * @return boolean
	 */
	public boolean isLocalProtectionInUse() {
		return this.localProtectionInUse;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.localProtectionAvailable ? 1231 : 1237);
		result = prime * result + (this.localProtectionInUse ? 1231 : 1237);
		result = prime * result + ((this.prefix == null) ? 0 : this.prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final RROIPAddressSubobject other = (RROIPAddressSubobject) obj;
		if (this.localProtectionAvailable != other.localProtectionAvailable)
			return false;
		if (this.localProtectionInUse != other.localProtectionInUse)
			return false;
		if (this.prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!this.prefix.equals(other.prefix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RROIPAddressSubobject [prefix=");
		builder.append(this.prefix);
		builder.append(", lpa=");
		builder.append(this.localProtectionAvailable);
		builder.append(", lpiu=");
		builder.append(this.localProtectionInUse);
		builder.append("]");
		return builder.toString();
	}

}
