/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.protocol.concepts.Identifier;

/**
 * Autonomous System (AS) Number. This is an Internet-level concept, where each
 * AS number identifies an administrative domain.
 * @see <a href="http://en.wikipedia.org/wiki/Autonomous_System_%28Internet%29">Wikipedia article</a>
 */
@Immutable
public final class ASNumber implements Comparable<ASNumber>, Identifier {
	/**
	 * AS_TRANS as defined by RFC4893 and RFC6793.
	 */
	public static final ASNumber TRANS = new ASNumber(23456);
	private static final long serialVersionUID = 7654204313238963136L;
	private final int highValue, lowValue;

	/**
	 * Create an AS number representation from a pair of 16bit integers,
	 * representing high/low components, for example as carried in ASDOT+
	 * notation.
	 *
	 * @param highValue Value of high-order 16 bits
	 * @param lowValue Value of low-order 16 bits
	 * @throws
	 *     @li IllegalArgumentException when either highValue or lowValue
	 *         out of allowed range (0-65535)
	 */
	public ASNumber(final int highValue, final int lowValue) {
		if (highValue < 0 || highValue > 65535)
			throw new IllegalArgumentException("Invalid high-order value");
		if (lowValue < 0 || lowValue > 65535)
			throw new IllegalArgumentException("Invalid low-order value");

		this.highValue = highValue;
		this.lowValue = lowValue;
	}

	/**
	 * Create an AS number representation from a single integer. The
	 * integer is the 'raw' AS number value, with unsigned 32bit range,
	 * for example as carried in PLAIN notation.
	 *
	 * @param asn Raw AS number
	 * @throws
	 *     @li IllegalArgumentException when asn is out of allowed range
	 *         (0-4294967295)
	 */
	public ASNumber(final long asn) {
		if (asn < 0 || asn > 4294967295L)
			throw new IllegalArgumentException("Invalid AS number");

		this.highValue = (int) (asn / 65536);
		this.lowValue = (int) (asn % 65536);
	}

	/**
	 * Get the AS number as a single integer. For 2-octet AS numbers,
	 * only low 1 bits are used.
	 *
	 * @return Raw AS number
	 */
	public long getAsn() {
		return highValue * 65536L + lowValue;
	}

	/**
	 * Get the high-valued part of the AS number. For 2-octet AS numbers,
	 * this will always be 0.
	 *
	 * @return High-order bits, guaranteed to be in 0-65535 range.
	 */
	public int getHighValue() {
		return highValue;
	}

	/**
	 * Get the low-valued part of the AS number. For 2-octet AS numbers,
	 * this value is equal to the AS number.
	 *
	 * @return Low-order bits, guaranteed to be in 0-65535 range.
	 */
	public int getLowValue() {
		return lowValue;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ASNumber))
			return false;

		final ASNumber as = (ASNumber)o;
		return lowValue == as.lowValue && highValue == as.highValue;
	}

	@Override
	public int hashCode() {
		return 7 * lowValue + 13 * highValue;
	}

	@Override
	public int compareTo(final ASNumber as) {
		if (highValue != as.highValue)
			return highValue - as.highValue;
		if (lowValue != as.lowValue)
			return lowValue - as.lowValue;
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Output string is formated according in ASDOT notation.
	 * @see <a href="http://tools.ietf.org/html/rfc5396">RFC5396</a>
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		if (highValue != 0) {
			sb.append(highValue);
			sb.append('.');
		}
		sb.append(lowValue);
		return sb.toString();
	}
}

