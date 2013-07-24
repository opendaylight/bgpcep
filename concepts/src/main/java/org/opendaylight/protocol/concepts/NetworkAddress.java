/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import org.opendaylight.protocol.concepts.Identifier;

/**
 * Generic identifier of a network entity. A network entity is typically a
 * host. Hosts may be contained within a network -- which is represented by
 * a @sa Prefix. To support operation within the context of a prefix,
 * NetworkAddresses have to support a concept of 'masking', where all bits
 * beyond first <n> bit are set to 0 and any bytes are trimmed.
 *
 * @param <T> subtype of Network Address
 */
public interface NetworkAddress<T extends NetworkAddress<?>> extends Comparable<T>, Identifier {
	/**
	 * Returns the raw address of this object. The result is in network
	 * byte order: the highest order byte of the address is in
	 * getAddress()[0].
	 * 
	 * @return the raw address of this object, guaranteed to be non-null
	 */
	public byte[] getAddress();

	/**
	 * Apply a mask of first #bits, setting the rest to zero.
	 *
	 * @param bits Number of bits to keep in place. Has to be
	 *                 non-negative.
	 * @return A new NetworkAddress instance.
	 */
	public T applyMask(int bits);

	/**
	 * Convert the address into a prefix of specified length.
	 *
	 * @param length Prefix length
	 * @return A prefix representation of this address.
	 */
	public Prefix<T> asPrefix(int length);

	/**
	 * Convert the address into a host prefix, e.g. a prefix with length
	 * equal to the address bitsize.
	 *
	 * @return A prefix representation of this address.
	 */
	public Prefix<T> asHostPrefix();

	/**
	 * Returns the address family class.
	 * 
	 * @return address family class
	 */
	public AddressFamily<T> getAddressFamily();
}

