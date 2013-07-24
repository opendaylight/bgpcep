/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.IPv6Address;

/**
 * Specific structure of IPv6 Extended Tunnel Identifier.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 */
public final class IPv6ExtendedTunnelIdentifier extends AbstractExtendedTunnelIdentifier<IPv6Address> {

	private static final long serialVersionUID = -4603732260818370518L;

	/**
	 * Creates IPv6ExtendedTunnelIdentifier with given IPv6Address.
	 * @param routerAddress {@link IPv6Address}
	 */
	public IPv6ExtendedTunnelIdentifier(final IPv6Address routerAddress) {
		super(routerAddress);
	}
}
