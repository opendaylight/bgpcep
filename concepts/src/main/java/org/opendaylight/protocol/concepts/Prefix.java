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
 * A network prefix object. Prefix is a networking concept grouping together
 * a set of addresses into a 'subnet', such that their attributes can be easily
 * expressed. A prefix is formed by a base NetworkAddress and number of leading
 * bits which are considered significant. An address is considered to be a part
 * of a prefix if it differes only in insignificant (right-most) bits.
 *
 * @param <T> template reference to a Network Address class
 */
public interface Prefix<T extends NetworkAddress<?>> extends Identifier {
	/**
	 * Get the base address of the prefix.
	 *
	 * @return Base network address
	 */
	public T getAddress();

	/**
	 * The the length of significant part of the base address.
	 *
	 * @return Prefix length
	 */
	public int getLength();

	/**
	 * Check if a Prefix contains a particular network address.
	 *
	 * @param address Network address to check for membership
	 * @return true if address belongs to this subnet, false otherwise
	 */
	public boolean contains(NetworkAddress<?> address);

	/**
	 * Returns the address family class.
	 * 
	 * @return address family class
	 */
	public AddressFamily<T> getAddressFamily();
}

