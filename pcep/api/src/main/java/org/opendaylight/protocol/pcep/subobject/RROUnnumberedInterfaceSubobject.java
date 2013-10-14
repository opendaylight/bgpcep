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
 * Structure of unnumbered Iterface Subobject.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3477">Section 4: Signalling Unnumbered Links in EROs</a>
 */
public class RROUnnumberedInterfaceSubobject extends ReportedRouteSubobject {
	private final UnnumberedSubobject interfaceID;
	private final Ipv4Address routerID;

	/**
	 * Constructs new Unnumbered Interface Subobject.
	 * 
	 * @param routerID IPv4Address
	 * @param interfaceID UnnumberedInterfaceIdentifier
	 */
	public RROUnnumberedInterfaceSubobject(final Ipv4Address routerID, final UnnumberedSubobject interfaceID) {
		super();
		this.routerID = routerID;
		this.interfaceID = interfaceID;
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
	 * Gets {@link UnnumberedSubobject} representation of Interface ID.
	 * 
	 * @return UnnumberedSubobject
	 */
	public UnnumberedSubobject getInterfaceID() {
		return this.interfaceID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.interfaceID == null) ? 0 : this.interfaceID.hashCode());
		result = prime * result + ((this.routerID == null) ? 0 : this.routerID.hashCode());
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
		final RROUnnumberedInterfaceSubobject other = (RROUnnumberedInterfaceSubobject) obj;
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
		builder.append("RROUnnumberedInterfaceSubobject [interfaceID=");
		builder.append(this.interfaceID);
		builder.append(", routerID=");
		builder.append(this.routerID);
		builder.append("]");
		return builder.toString();
	}

}
