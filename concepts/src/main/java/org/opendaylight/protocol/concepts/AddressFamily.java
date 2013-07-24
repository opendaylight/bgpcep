/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.Set;

/**
 * Interface marking an Address Family concept. An address family defines
 * a NetworkAddress type, corresponding Prefix type as well as their textual
 * representations.
 * 
 * @param <T> Network Address type
 */
public interface AddressFamily<T extends NetworkAddress<?>> {
	/**
	 * Attempt to parse an address from a string. 
	 * 
	 * @param string String representation of the address
	 * @return A network address instance
	 * @throws IllegalArgumentException if string is null or does not conform
	 *         to address encoding rules.
	 */
	public T addressForString(String string);

	/**
	 * Attempt to parse a parse from a string. 
	 * 
	 * @param string String representation of the prefix
	 * @return A prefix instance
	 * @throws IllegalArgumentException if string is null or does not conform
	 *         to prefix encoding rules.
	 * @throws NumberFormatException if the prefix length is not a parseable
	 *         number.
	 */
	public Prefix<T> prefixForString(String string);

	/**
	 * Create a network address from a its byte representation.
	 *
	 * @param bytes address as a byte array
	 * @return {@link NetworkAddress}
	 * @throws IllegalArgumentException if the provided byte array
	 *         does not conform to address formatting rules
	 */
	public T addressForBytes(byte[] bytes);

	/**
	 * Parse a byte array into a Prefix of specified length
	 *
	 * @param bytes array to be parsed to a Prefix
	 * @param bitLength
	 *            length of the prefix in bits, to be given as parameter when
	 *            initializing a Prefix
	 * @return new Prefix
	 */
	public Prefix<T> prefixForBytes(byte[] bytes, int bitLength);

	/**
	 * Parse given byte array to a list of prefixes.
	 *
	 * @param bytes byte array to be parsed.
	 * @return list of prefixes, if bytes is empty, returns empty list.
	 */
	public Set<? extends Prefix<T>> prefixListForBytes(byte[] bytes);

	/**
	 * Returns the number of bytes required for successful address
	 * reconstruction via {@link addressForBytes}
	 * 
	 * @return number of bytes required
	 */
	public int requiredBytes();
}

