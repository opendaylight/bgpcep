/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * Structure of Autonomous System Number Subobject. Defined in RFC5521.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1">Exclude Route Object definition</a>
 */
public class XROAsNumberSubobject extends ExcludeRouteSubobject {

	private final AsNumber asnumber;

	/**
	 * Constructs new ASNumber Subobject.
	 * 
	 * @param asnumber ASNumber
	 * @param mandatory boolean
	 */
	public XROAsNumberSubobject(final AsNumber asnumber, final boolean mandatory) {
		super(mandatory);
		this.asnumber = asnumber;
	}

	/**
	 * Gets {@link AsNumber}.
	 * 
	 * @return ASNumber
	 */
	public AsNumber getASNumber() {
		return this.asnumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.asnumber == null) ? 0 : this.asnumber.hashCode());
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
		final XROAsNumberSubobject other = (XROAsNumberSubobject) obj;
		if (this.asnumber == null) {
			if (other.asnumber != null)
				return false;
		} else if (!this.asnumber.equals(other.asnumber))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("XROAsNumberSubobject [asnumber=");
		builder.append(this.asnumber);
		builder.append(", mandatory=");
		builder.append(this.mandatory);
		builder.append("]");
		return builder.toString();
	}

}
