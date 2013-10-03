/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import com.google.common.net.InetAddresses;

/**
 * Utility class for dealing with various IP-based network addresses.
 */
public final class IPAddresses {
	/**
	 * A set containing all IP address families.
	 */
	public static final Set<AddressFamily<?>> FAMILIES;

	static {
		FAMILIES = new HashSet<>(2);
		FAMILIES.add(IPv4.FAMILY);
		FAMILIES.add(IPv6.FAMILY);
	}

	private IPAddresses() {
	}

	/**
	 * Instantiate a network address from its string representation.
	 * 
	 * @param string string representation
	 * @return network address parsed from the string
	 * @throws IllegalArgumentException if the string failed to parse into any of the supported classes.
	 */
	public static NetworkAddress<?> parseNetworkAddress(final String string) {
		final InetAddress a = InetAddresses.forString(string);
		if (a instanceof Inet4Address)
			return new IPv4Address(a);
		if (a instanceof Inet6Address)
			return new IPv6Address(a);
		throw new IllegalArgumentException("Unsupported network address");
	}

	public static NetworkAddress<?> createNetworkAddress(final InetAddress inetAddress) {
		checkNotNull(inetAddress);
		if (inetAddress instanceof Inet4Address) {
			return new IPv4Address(inetAddress);
		} else if (inetAddress instanceof Inet6Address) {
			return new IPv6Address(inetAddress);
		} else {
			throw new IllegalStateException("Unknown InetAddress " + inetAddress);
		}
	}
}
