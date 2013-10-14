/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.pcep.concepts.ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;

/**
 * Specific structure for IPv4 LSP Identifier TLV.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 */
public final class IPv4LSPIdentifiersTlv extends AbstractLSPIdentifiersTlv<IPv4Address> {
	private static final long serialVersionUID = -8249620306610957898L;

	/**
	 * Constructs new IPv4 LSP Identifiers TLV.
	 * 
	 * @param senderAddress
	 *            {@link IPv4Address}
	 * @param lspID
	 *            {@link LSPIdentifier}
	 * @param tunnelID
	 *            {@link TunnelIdentifier}
	 * @param extendedTunnelID
	 *            {@link ExtendedTunnelIdentifier}
	 */
	public IPv4LSPIdentifiersTlv(IPv4Address senderAddress, LSPIdentifier lspID, TunnelIdentifier tunnelID,
			ExtendedTunnelIdentifier<IPv4Address> extendedTunnelID) {
		super(senderAddress, lspID, tunnelID, extendedTunnelID);
	}
}
