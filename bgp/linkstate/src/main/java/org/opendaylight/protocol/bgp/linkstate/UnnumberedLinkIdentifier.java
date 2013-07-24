/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

/**
 * Identifier class for Unnumbered Links, as defined by
 * <a href="http://tools.ietf.org/html/rfc4202">RFC 4202</a>. Note that the
 * class naming is a deliberate misnomer.
 */
public final class UnnumberedLinkIdentifier implements Comparable<UnnumberedLinkIdentifier>, InterfaceIdentifier {
	private static final long serialVersionUID = -8301403294494612786L;
	private final long value;

	/**
	 * Create a new unnumbered link identifier.
	 *
	 * @param value Raw link identifier, has to be in range 1-4294967295
	 * @throws IllegalArgumentException when value is outside of allowed range
	 */
	public UnnumberedLinkIdentifier(final long value) {
		if (value < 1 || value > 4294967295L)
			throw new IllegalArgumentException("Unsupported identifier value");
		this.value = value;
	}

	/**
	 * Returns raw link identifier value.
	 *
	 * @return value Raw link identifier value
	 */
	public long getValue() {
		return  this.value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.value ^ (this.value >>> 32));
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
		final UnnumberedLinkIdentifier other = (UnnumberedLinkIdentifier) obj;
		if (this.value != other.value)
			return false;
		return true;
	}

	@Override
	public int compareTo(final UnnumberedLinkIdentifier other) {
		if (this == other)
			return 0;
		if (other == null)
			return 1;
		if (this.value < other.value)
			return -1;
		if (this.value > other.value)
			return 1;
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("UnnumberedLinkIdentifier [value=");
		builder.append(this.value);
		builder.append("]");
		return builder.toString();
	}
}


