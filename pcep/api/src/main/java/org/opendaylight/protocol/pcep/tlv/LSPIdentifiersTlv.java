/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.pcep.concepts.ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Interface defining basic LSPIdentifiersTLV.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 * @param <T>
 */
public interface LSPIdentifiersTlv<T extends NetworkAddress<T>> extends Tlv {

	/**
	 * Gets specific senders {@link NetworkAddress}.
	 * 
	 * @return T sender network address
	 */
	public T getSenderAddress();

	/**
	 * Gets {@link LSPIdentifier}.
	 * 
	 * @return LSPIdentifier
	 */
	public LSPIdentifier getLspID();

	/**
	 * Gets {@link TunnelIdentifier}.
	 * 
	 * @return TunnelIdentifier
	 */
	public TunnelIdentifier getTunnelID();

	/**
	 * Gets specific {@link ExtendedTunnelIdentifier}.
	 * 
	 * 
	 * @return ExtendedTunnelIdentifier
	 */
	public ExtendedTunnelIdentifier<T> getExtendedTunnelID();
}
