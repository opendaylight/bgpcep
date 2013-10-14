/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.IPv4Address;

/**
 * Specific structure of IPv4 Extended Tunnel Identifier.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 */
public final class IPv4ExtendedTunnelIdentifier extends AbstractExtendedTunnelIdentifier<IPv4Address> {

	private static final long serialVersionUID = -8872936514548777175L;

	/**
	 * Creates IPv4ExtendedTunnelIdentifier with given IPv4Address.
	 * @param routerAddress {@link IPv4Address}
	 */
	public IPv4ExtendedTunnelIdentifier(final IPv4Address routerAddress) {
		super(routerAddress);
	}
}
