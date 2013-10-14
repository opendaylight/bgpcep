/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.pcep.PCEPTlv;

/**
 * Structure of PCE Stateful Capability Tlv.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-8.6">STATEFUL-PCE-CAPABILITY
 *      TLV</a>
 * @see <a
 * 		href="http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt#section-4.1">Stateful PCE Capability
 * 		TLV</a>
 *
 */
public class PCEStatefulCapabilityTlv implements PCEPTlv {

	private static final long serialVersionUID = 5567589958323130325L;

	private final boolean update;

	private final boolean versioned;

	private final boolean instantiated;

	/**
	 * Constructs PCE Stateful Capability Tlv
	 *
	 * @param update
	 *            boolean
	 * @param versioned
	 *            boolean
	 */
	public PCEStatefulCapabilityTlv(boolean instantiated, boolean update, boolean versioned) {
		this.instantiated = instantiated;
		this.update = update;
		this.versioned = versioned;
	}

	/**
	 * Setting of Instantiated flag.
	 *
	 * @return boolean
	 */
	public boolean isInstantiated() {
		return this.instantiated;
	}

	/**
	 * Setting of Update flag.
	 *
	 * @return boolean
	 */
	public boolean isUpdate() {
		return this.update;
	}

	/**
	 * Setting of Versioned flag.
	 *
	 * @return boolean
	 */
	public boolean isVersioned() {
		return this.versioned;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.instantiated ? 1231 : 1237);
		result = prime * result + (this.update ? 1231 : 1237);
		result = prime * result + (this.versioned ? 1231 : 1237);
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
		if (!(obj instanceof PCEStatefulCapabilityTlv))
			return false;
		final PCEStatefulCapabilityTlv other = (PCEStatefulCapabilityTlv) obj;
		if (this.instantiated != other.instantiated)
			return false;
		if (this.update != other.update)
			return false;
		if (this.versioned != other.versioned)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEStatefulCapabilityTlv [update=");
		builder.append(this.update);
		builder.append(", versioned=");
		builder.append(this.versioned);
		builder.append(", instantiated=");
		builder.append(this.instantiated);
		builder.append("]");
		return builder.toString();
	}
}
