/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;

/**
 * DTO for a subtype of Next Hop, containing type IPv4 addresses.
 */
public final class IPv4NextHop extends AbstractNextHop<IPv4Address> {
	private static final long serialVersionUID = -7013857868462257974L;

	/**
	 * Construct a new IPv4-based next hop.
	 * 
	 * @param global {@link IPv4Address}
	 */
	public IPv4NextHop(final IPv4Address global) {
		super(global, null);
	}

	/**
	 * Creates a new IPv4NextHop using string IPv4Address.
	 * 
	 * @param string String representation of IPv4Address.
	 * @return new IPv4NextHop
	 */
	public static IPv4NextHop forString(final String string) {
		return new IPv4NextHop(IPv4.FAMILY.addressForString(string));
	}
}
