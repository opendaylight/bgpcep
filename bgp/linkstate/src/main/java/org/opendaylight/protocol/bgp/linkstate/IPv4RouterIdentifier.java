/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import com.google.common.base.Preconditions;

public final class IPv4RouterIdentifier extends AbstractNetworkAddressRouterIdentifier<IPv4Address> {
	private static final long serialVersionUID = 1L;

	public IPv4RouterIdentifier(final IPv4Address address) {
		super(address);
	}

	public static IPv4RouterIdentifier forString(final String string) {
		Preconditions.checkNotNull(string);
		return new IPv4RouterIdentifier(IPv4.FAMILY.addressForString(string));
	}

	public static IPv4RouterIdentifier forBytes(final byte[] bytes) {
		Preconditions.checkNotNull(bytes);
		return new IPv4RouterIdentifier(IPv4.FAMILY.addressForBytes(bytes));
	}
}
