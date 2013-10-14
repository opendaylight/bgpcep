/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.pcep.PCEPTlv;

/**
 *	Structure of No Path Vector TLV.
 *
 *	@see <a href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-protection-00#section-4.3"
 *			Bypass Tlv</a>
 */
public class ByPassTlv implements PCEPTlv {

	private static final long serialVersionUID = 5879892226322401651L;

	private final boolean nodeProtection;

	private final boolean localProtectionInUse;

	private final IPv4Address bypassAddress;

	/**
	 * Constructs ByPass Tlv.
	 *
	 * @param nodeProtection
	 * 		boolean
	 * @param localProtectionInUse
	 * 		boolean
	 * @param bypassAddress
	 *		IPv4Address
	 */
	public ByPassTlv(final boolean nodeProtection, final boolean localProtectionInUse,
			final IPv4Address bypassAddress) {
		this.nodeProtection = nodeProtection;
		this.localProtectionInUse = localProtectionInUse;
		this.bypassAddress = bypassAddress;
	}

	/**
	 * The N Flag indicates whether the Bypass is used for node-protection.
	 * If the N flag is set to 1, the Bypass is used for node-protection.
	 * If the N flag is 0, the Bypass is used for link-protection.
	 *
	 * @return the nodeProtection
	 */
	public final boolean isNodeProtection() {
		return this.nodeProtection;
	}

	/**
	 * The I Flag indicates that local repair mechanism is in use.
	 *
	 * @return the localProtectionInUse
	 */
	public final boolean isLocalProtectionInUse() {
		return this.localProtectionInUse;
	}

	/**
	 * For link protection, the Bypass IPv4 Address is
     * the nexthop address of the protected link in the paths of the
     * protected LSPs.  For node protection, the Bypass IPv4 Address is
     * the node addresses of the protected node.
     *
	 * @return the bypassAddress
	 */
	public final IPv4Address getBypassAddress() {
		return this.bypassAddress;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ByPassTlv [nodeProtection=");
		builder.append(this.nodeProtection);
		builder.append(", localProtectionInUse=");
		builder.append(this.localProtectionInUse);
		builder.append(", bypassAddress=");
		builder.append(this.bypassAddress);
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.bypassAddress == null) ? 0 : this.bypassAddress.hashCode());
		result = prime * result + (this.localProtectionInUse ? 1231 : 1237);
		result = prime * result + (this.nodeProtection ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ByPassTlv))
			return false;
		final ByPassTlv other = (ByPassTlv) obj;
		if (this.bypassAddress == null) {
			if (other.bypassAddress != null)
				return false;
		} else if (!this.bypassAddress.equals(other.bypassAddress))
			return false;
		if (this.localProtectionInUse != other.localProtectionInUse)
			return false;
		if (this.nodeProtection != other.nodeProtection)
			return false;
		return true;
	}
}
