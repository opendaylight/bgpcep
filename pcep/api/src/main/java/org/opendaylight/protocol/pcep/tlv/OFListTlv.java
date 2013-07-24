/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPTlv;

/**
 * It MAY be carried within an OPEN object sent by a PCE in an Open message to a
 * PCEP peer so as to indicate the list of supported objective functions.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5541#section-2.1">OF-List TLV</a>
 */
public class OFListTlv implements PCEPTlv {
	private static final long serialVersionUID = 3409582385994162451L;

	private final List<PCEPOFCodes> ofCodes;

	/**
	 * Constructs new objective functions list tlv
	 * 
	 * @param ofCodes
	 *            lit of objective functions
	 */
	public OFListTlv(List<PCEPOFCodes> ofCodes) {
		super();
		this.ofCodes = ofCodes;
	}

	/**
	 * Gets list of objective functions
	 * 
	 * @return list of objective functions
	 */
	public List<PCEPOFCodes> getOfCodes() {
		return this.ofCodes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.ofCodes == null) ? 0 : this.ofCodes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final OFListTlv other = (OFListTlv) obj;
		if (this.ofCodes == null) {
			if (other.ofCodes != null)
				return false;
		} else if (!this.ofCodes.equals(other.ofCodes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("OFListTlv [ofCodes=");
		builder.append(this.ofCodes);
		builder.append("]");
		return builder.toString();
	}

}
