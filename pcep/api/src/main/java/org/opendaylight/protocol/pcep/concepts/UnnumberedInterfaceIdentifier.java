/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.Identifier;

/**
 * A 32-bit (unsigned) identifying an interface within a router. This identifier
 * is explicitly local to the router, e.g. each router has its own namespace.
 */
public final class UnnumberedInterfaceIdentifier implements Comparable<UnnumberedInterfaceIdentifier>, Identifier {

	private static final long serialVersionUID = -8488014237579913120L;

	private final long interfaceId;

	/**
	 * Creates an instance of UnnumberedInterfaceIdentifier from long number.
	 * 
	 * @param interfaceId
	 *            long the value of the UnnumberedInterfaceIdentifier
	 */
	public UnnumberedInterfaceIdentifier(final long interfaceId) {
		if (interfaceId < 0 || interfaceId > 4294967295L)
			throw new IllegalArgumentException("Invalid link identifier");
		this.interfaceId = interfaceId;
	}

	/**
	 * Getter for Interface Id represented as long.
	 * 
	 * @return long representation of Interface Id. From 0 to 4294967295.
	 */
	public long getInterfaceId() {
		return this.interfaceId;
	}

	@Override
	public int compareTo(final UnnumberedInterfaceIdentifier o) {
		if (this.interfaceId < o.getInterfaceId())
			return -1;
		if (this.interfaceId > o.getInterfaceId())
			return 1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.interfaceId ^ (this.interfaceId >>> 32));
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
		final UnnumberedInterfaceIdentifier other = (UnnumberedInterfaceIdentifier) obj;
		if (this.interfaceId != other.interfaceId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("UnnumberedInterfaceIdentifier [interfaceId=");
		builder.append(this.interfaceId);
		builder.append("]");
		return builder.toString();
	}
}
