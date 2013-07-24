/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import com.google.common.base.Preconditions;

public final class IPv4InterfaceIdentifier implements Comparable<IPv4InterfaceIdentifier>, InterfaceIdentifier {
	private static final long serialVersionUID = 1L;
	private final IPv4Address value;

	/**
	 * Create a new unnumbered link identifier.
	 * 
	 * @param value Raw link identifier, has to be in range 1-4294967295
	 * @throws IllegalArgumentException when value is outside of allowed range
	 */
	public IPv4InterfaceIdentifier(final IPv4Address value) {
		Preconditions.checkNotNull(value, "Unsupported identifier value");
		this.value = value;
	}

	/**
	 * Returns raw link identifier value.
	 * 
	 * @return value Raw link identifier value
	 */
	public IPv4Address getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final IPv4InterfaceIdentifier other = (IPv4InterfaceIdentifier) obj;
		if (!this.value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public int compareTo(final IPv4InterfaceIdentifier other) {
		if (this == other)
			return 0;
		if (other == null)
			return 1;
		return this.value.compareTo(other.value);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("IPv4InterfaceIdentifier [value=");
		builder.append(this.value);
		builder.append("]");
		return builder.toString();
	}

	public static IPv4InterfaceIdentifier forString(final String string) {
		Preconditions.checkNotNull(string);
		return new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForString(string));
	}

	public static IPv4InterfaceIdentifier forBytes(final byte[] bytes) {
		Preconditions.checkNotNull(bytes);
		return new IPv4InterfaceIdentifier(IPv4.FAMILY.addressForBytes(bytes));
	}
}
