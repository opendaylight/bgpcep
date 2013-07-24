/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.Identifier;

/**
 * Identifier class for Administrative Groups. Administrative groups, also
 * known as Colors, are a way for administrative authority to assign objects
 * to groups. This allows separation of policy, which defines what actions
 * should be applied to a particular group from actual assignment of objects.
 */
public class AdministrativeGroup implements Comparable<AdministrativeGroup>, Identifier {
	public static final AdministrativeGroup NONE = new AdministrativeGroup(0);
	private static final long serialVersionUID = 6127162286553485280L;
	private final long value;

	/**
	 * Create a new administrative group. Each group has a numeric
	 * identifier in range 0-4294967295. Two groups with the same
	 * identifier are considered equal.
	 * 
	 * @param value Group identifier value
	 */
	public AdministrativeGroup(final long value) {
		if (value < 0 || value > 4294967295L)
			throw new IllegalArgumentException("Invalid Administrative Group value");
		this.value = value;
	}

	/**
	 * Standard getter for value attribute of {@link AdministrativeGroup}.
	 *
	 * @return Group identifier value
	 */
	public final long getValue() {
		return value;
	}

	@Override
	public final int compareTo(final AdministrativeGroup other) {
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
		if (!(obj instanceof AdministrativeGroup))
			return false;
		final AdministrativeGroup other = (AdministrativeGroup) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}

