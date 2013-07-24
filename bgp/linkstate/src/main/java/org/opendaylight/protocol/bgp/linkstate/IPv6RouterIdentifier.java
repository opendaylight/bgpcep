/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import com.google.common.base.Preconditions;

public final class IPv6RouterIdentifier extends AbstractNetworkAddressRouterIdentifier<IPv6Address> {
	private static final long serialVersionUID = 1L;

	public IPv6RouterIdentifier(final IPv6Address address) {
		super(address);
	}

	public static IPv6RouterIdentifier forString(final String string) {
		Preconditions.checkNotNull(string);
		return new IPv6RouterIdentifier(IPv6.FAMILY.addressForString(string));
	}

	public static IPv6RouterIdentifier forBytes(final byte[] bytes) {
		Preconditions.checkNotNull(bytes);
		return new IPv6RouterIdentifier(IPv6.FAMILY.addressForBytes(bytes));
	}
}
