/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;


/**
 * Class representing a Shared Risk Link Group identifier. A link may be
 * part of multiple such groups. Membership in such a group indicates that
 * members share a common risk (such as single wire, etc.).
 */
public class SharedRiskLinkGroup implements Comparable<SharedRiskLinkGroup>, Identifier {
	private static final long serialVersionUID = -760766663668819360L;
	private final long value;

	/**
	 * Create a new group with a particular value. Valid values are in
	 * the range of 0-4294967295. Two groups with the same value are
	 * considered equal.
	 * @param value SRLG value
	 */
	public SharedRiskLinkGroup(final long value) {
		if (value < 0 || value > 4294967295L)
			throw new IllegalArgumentException("Invalid SRLG value");
		this.value = value;
	}

	/**
	 * Returns value attribute of {@link SharedRiskLinkGroup}.
	 * @return value of {@link SharedRiskLinkGroup}
	 */
	public long getValue() {
		return value;
	}

	@Override
	public int compareTo(final SharedRiskLinkGroup other) {
		if (value < other.value)
			return -1;
		if (value > other.value)
			return 1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SharedRiskLinkGroup))
			return false;
		final SharedRiskLinkGroup other = (SharedRiskLinkGroup) obj;
		if (value != other.value)
			return false;
		return true;
	}
}

