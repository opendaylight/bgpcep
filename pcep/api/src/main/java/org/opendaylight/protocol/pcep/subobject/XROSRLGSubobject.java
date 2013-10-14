/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

/**
 * Structure of Shared Risk Link Group Subobject. Defined in RFC5521.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1">Exclude Route Object definition</a>
 */
public class XROSRLGSubobject extends ExcludeRouteSubobject {

	private final XROSubobjectAttribute attribute;

	// private final SharedRiskLinkGroup srlgId;

	/**
	 * Constructs new Shared Risk Link Group Subobject.
	 * 
	 * @param srlgId SharedRiskLinkGroup
	 * @param mandatory boolean
	 */
	public XROSRLGSubobject(final boolean mandatory) {
		super(mandatory);
		this.attribute = XROSubobjectAttribute.SRLG;
		// this.srlgId = srlgId;
	}

	/**
	 * Gets the Shared Risk Link Group.
	 * 
	 * @return SharedRiskLinkGroup
	 */
	// public SharedRiskLinkGroup getSrlgId() {
	// return this.srlgId;
	// }

	/**
	 * Gets the attribute of the subobject
	 * 
	 * @return the attribute
	 */
	public XROSubobjectAttribute getAttribute() {
		return this.attribute;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("XROSRLGSubobject [attribute=");
		builder.append(this.attribute);
		builder.append(", mandatory=");
		builder.append(this.mandatory);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.attribute == null) ? 0 : this.attribute.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final XROSRLGSubobject other = (XROSRLGSubobject) obj;
		if (this.attribute != other.attribute)
			return false;
		return true;
	}

}
