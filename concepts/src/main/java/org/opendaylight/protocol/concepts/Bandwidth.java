/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

/**
 * Bandwidth concept class. It represents network bandwidth in bytes per second.
 * The precision of this class is limited to single-precision floating-point.
 */
@Immutable
public final class Bandwidth implements Comparable<Bandwidth>, Serializable {
	public static final Bandwidth ZERO = new Bandwidth(0);
	private static final long serialVersionUID = 5795612451568505103L;
	private final double bytesPerSecond;

	/**
	 * Constructor of {@link Bandwidth} object.
	 * @param bytesPerSecond Bandwidth capacity, expressed as bytes per second
	 */
	public Bandwidth(final double bytesPerSecond) {
		this.bytesPerSecond = bytesPerSecond;
	}

	/**
	 * Standard getter for bytesPerSecond attribute of {@link Bandwidth}.
	 * @return bytesPerSecond Bandwidth capacity, expressed as bytes per second
	 */
	public double getBytesPerSecond() {
		return bytesPerSecond;
	}

	/**
	 * Standard getter for bitsPerSecond attribute of {@link Bandwidth}.
	 * @return bitsPerSecond Bandwidth capacity, expressed as bytes per second
	 */
	public long getBitsPerSecond() {
		return (long) (8 * bytesPerSecond);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(bytesPerSecond);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bandwidth other = (Bandwidth) obj;
		if (Double.doubleToLongBits(bytesPerSecond) != Double
				.doubleToLongBits(other.bytesPerSecond))
			return false;
		return true;
	}

	@Override
	public int compareTo(final Bandwidth b) {
		if (bytesPerSecond < b.bytesPerSecond)
			return -1;
		if (bytesPerSecond > b.bytesPerSecond)
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Bandwidth [bytesPerSecond=");
		sb.append(bytesPerSecond);
		sb.append(']');
		return sb.toString();
	}
}

