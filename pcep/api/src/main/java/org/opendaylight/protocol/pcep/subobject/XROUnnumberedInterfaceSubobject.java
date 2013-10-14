/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;

/**
 * Structure of unnumbered Interface Subobject. Defined in RFC5521.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1">Exclude Route Object definition</a>
 */
public class XROUnnumberedInterfaceSubobject extends ExcludeRouteSubobject {

	private final XROSubobjectAttribute attribute;

	private final UnnumberedSubobject interfaceID;

	private final Ipv4Address routerID;

	/**
	 * Constructs new Unnumbered Interface Subobject.
	 * 
	 * @param routerID IPv4Address
	 * @param interfaceID UnnumberedInterfaceIdentifier
	 * @param mandatory boolean
	 * @param attribute XROSubobjectAttribute
	 */
	public XROUnnumberedInterfaceSubobject(final Ipv4Address routerID, final UnnumberedSubobject interfaceID, final boolean mandatory,
			final XROSubobjectAttribute attribute) {
		super(mandatory);
		this.attribute = attribute;
		this.routerID = routerID;
		this.interfaceID = interfaceID;
	}

	/**
	 * Gets the attribute of the subobject
	 * 
	 * @return the attribute
	 */
	public XROSubobjectAttribute getAttribute() {
		return this.attribute;
	}

	/**
	 * Gets {@link IPv4Address} representation of router ID.
	 * 
	 * @return IPv4Address
	 */
	public Ipv4Address getRouterID() {
		return this.routerID;
	}

	/**
	 * Gets {@link UnnumberedInterfaceIdentifier} representation of Interface ID.
	 * 
	 * @return UnnumberedInterfaceIdentifier
	 */
	public UnnumberedSubobject getInterfaceID() {
		return this.interfaceID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.attribute == null) ? 0 : this.attribute.hashCode());
		result = prime * result + ((this.interfaceID == null) ? 0 : this.interfaceID.hashCode());
		result = prime * result + ((this.routerID == null) ? 0 : this.routerID.hashCode());
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
		final XROUnnumberedInterfaceSubobject other = (XROUnnumberedInterfaceSubobject) obj;
		if (this.attribute != other.attribute)
			return false;
		if (this.interfaceID == null) {
			if (other.interfaceID != null)
				return false;
		} else if (!this.interfaceID.equals(other.interfaceID))
			return false;
		if (this.routerID == null) {
			if (other.routerID != null)
				return false;
		} else if (!this.routerID.equals(other.routerID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("XROUnnumberedInterfaceSubobject [attribute=");
		builder.append(this.attribute);
		builder.append(", interfaceID=");
		builder.append(this.interfaceID);
		builder.append(", routerID=");
		builder.append(this.routerID);
		builder.append(", mandatory=");
		builder.append(this.mandatory);
		builder.append("]");
		return builder.toString();
	}

}
