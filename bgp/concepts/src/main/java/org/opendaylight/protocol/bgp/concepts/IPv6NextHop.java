/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;

/**
 * DTO for a subtype of NextHop, containing type IPv6 addresses.
 */
public final class IPv6NextHop extends AbstractNextHop<IPv6Address> {
	private static final long serialVersionUID = -6656906151774744248L;

	/**
	 * Construct a new IPv6 nexhop with only a global address.
	 * 
	 * @param global Globally-visible address
	 */
	public IPv6NextHop(final IPv6Address global) {
		this(global, null);
	}

	/**
	 * Construct a new IPv6 nexhop with both global and link-local address.
	 * 
	 * @param global Globally-visible address
	 * @param linkLocal Link-local address
	 */
	public IPv6NextHop(final IPv6Address global, final IPv6Address linkLocal) {
		super(global, linkLocal);
	}

	/**
	 * Creates a new IPv6NextHop using string IPv6Address.
	 * 
	 * @param global String representation of global IPv6Address.
	 * @return new IPv6NextHop
	 */
	public static IPv6NextHop forString(final String global) {
		return new IPv6NextHop(IPv6.FAMILY.addressForString(global));
	}

	/**
	 * Creates a new IPv6NextHop using global and linkLocal string IPv6Addresses.
	 * 
	 * @param global String representation of global IPv6Address
	 * @param linklocal String representation of linkLocal IPv6Address
	 * @return new IPv6NextHop
	 */
	public static IPv6NextHop forString(final String global, final String linkLocal) {
		return new IPv6NextHop(IPv6.FAMILY.addressForString(global), IPv6.FAMILY.addressForString(linkLocal));
	}
}
