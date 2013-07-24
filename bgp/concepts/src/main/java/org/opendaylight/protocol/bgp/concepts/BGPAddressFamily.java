/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.AddressFamily;

/**
 * Enumeration of all supported address families.
 */
public enum BGPAddressFamily {
	/**
	 * IP (IP version 4)
	 */
	IPv4(org.opendaylight.protocol.concepts.IPv4.FAMILY),
	/**
	 * IP6 (IP version 6)
	 */
	IPv6(org.opendaylight.protocol.concepts.IPv6.FAMILY),
	/**
	 * Link State Information
	 */
	LinkState(null);

	private final AddressFamily<?> family;

	private BGPAddressFamily(final AddressFamily<?> family) {
		this.family = family;
	}

	/**
	 * Return the associated address family class.
	 * 
	 * @return associated address family class
	 */
	public AddressFamily<?> getAddressFamily() {
		return family;
	}
}

