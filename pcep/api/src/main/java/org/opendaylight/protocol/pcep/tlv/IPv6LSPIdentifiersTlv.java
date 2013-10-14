/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.concepts.ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;

/**
 * Specific structure of IPv6 LSP Identifier TLV.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 */
public final class IPv6LSPIdentifiersTlv extends AbstractLSPIdentifiersTlv<IPv6Address> {
	private static final long serialVersionUID = 4188840025844510894L;

	/**
	 * Constructs new IPv6 LSP Identifiers TLV.
	 * 
	 * @param senderAddress
	 *            {@link IPv6Address}
	 * @param lspID
	 *            {@link LSPIdentifier}
	 * @param tunnelID
	 *            {@link TunnelIdentifier}
	 * @param extendedTunnelID
	 *            {@link ExtendedTunnelIdentifier}
	 */
	public IPv6LSPIdentifiersTlv(IPv6Address senderAddress, LSPIdentifier lspID, TunnelIdentifier tunnelID,
			ExtendedTunnelIdentifier<IPv6Address> extendedTunnelID) {
		super(senderAddress, lspID, tunnelID, extendedTunnelID);
	}
}
