/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Structure of Overloaded Duration Tlv.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.14">NOTIFICATION
 *      Object</a> - defined in text
 */
public class OverloadedDurationTlv implements Tlv {
	private final int value;

	/**
	 * Construct new Overloaded Duration Tlv.
	 * 
	 * @param value
	 *            int
	 */
	public OverloadedDurationTlv(int value) {
		this.value = value;
	}

	/**
	 * Gets Integer representation of Overloade Duration Value.
	 * 
	 * @return int
	 */
	public int getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.value;
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
		final OverloadedDurationTlv other = (OverloadedDurationTlv) obj;
		if (this.value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("OverloadedDurationTlv [value=");
		builder.append(this.value);
		builder.append("]");
		return builder.toString();
	}

}
