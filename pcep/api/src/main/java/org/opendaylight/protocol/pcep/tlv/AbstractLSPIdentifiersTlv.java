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

/**
 * Basic structure of LSP Identifiers TLV.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 * @param <T>
 */
public abstract class AbstractLSPIdentifiersTlv<T extends NetworkAddress<T>> implements LSPIdentifiersTlv<T> {
	private static final long serialVersionUID = 2386922658825295806L;

	private final T senderAddress;

	private final LSPIdentifier lspID;

	private final TunnelIdentifier tunnelID;

	private final ExtendedTunnelIdentifier<T> extendedTunnelID;

	/**
	 * Construct LSP Identifier TLV with mandatory objects.
	 *
	 * @param senderAddress
	 * @param lspID
	 * @param tunnelID
	 * @param extendedTunnelID
	 */
	protected AbstractLSPIdentifiersTlv(T senderAddress, LSPIdentifier lspID, TunnelIdentifier tunnelID, ExtendedTunnelIdentifier<T> extendedTunnelID) {
		if (senderAddress == null)
			throw new IllegalArgumentException("SenderAdress is mandatory.");
		this.senderAddress = senderAddress;

		if (lspID == null)
			throw new IllegalArgumentException("LspID is mandatory.");
		this.lspID = lspID;

		if (tunnelID == null)
			throw new IllegalArgumentException("TunnelID is mandatory.");
		this.tunnelID = tunnelID;

		if (extendedTunnelID == null)
			throw new IllegalArgumentException("ExtendedTunnelID is mandatory.");
		this.extendedTunnelID = extendedTunnelID;
	}

	@Override
	public T getSenderAddress() {
		return this.senderAddress;
	}

	@Override
	public LSPIdentifier getLspID() {
		return this.lspID;
	}

	@Override
	public TunnelIdentifier getTunnelID() {
		return this.tunnelID;
	}

	@Override
	public ExtendedTunnelIdentifier<T> getExtendedTunnelID() {
		return this.extendedTunnelID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.extendedTunnelID == null) ? 0 : this.extendedTunnelID.hashCode());
		result = prime * result + ((this.lspID == null) ? 0 : this.lspID.hashCode());
		result = prime * result + ((this.senderAddress == null) ? 0 : this.senderAddress.hashCode());
		result = prime * result + ((this.tunnelID == null) ? 0 : this.tunnelID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final AbstractLSPIdentifiersTlv<?> other = (AbstractLSPIdentifiersTlv<?>) obj;
		if (this.extendedTunnelID == null) {
			if (other.extendedTunnelID != null)
				return false;
		} else if (!this.extendedTunnelID.equals(other.extendedTunnelID))
			return false;
		if (this.lspID == null) {
			if (other.lspID != null)
				return false;
		} else if (!this.lspID.equals(other.lspID))
			return false;
		if (this.senderAddress == null) {
			if (other.senderAddress != null)
				return false;
		} else if (!this.senderAddress.equals(other.senderAddress))
			return false;
		if (this.tunnelID == null) {
			if (other.tunnelID != null)
				return false;
		} else if (!this.tunnelID.equals(other.tunnelID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AbstractLSPIdentifiersTlv [senderAddress=");
		builder.append(this.senderAddress);
		builder.append(", lspID=");
		builder.append(this.lspID);
		builder.append(", tunnelID=");
		builder.append(this.tunnelID);
		builder.append(", extendedTunnelID=");
		builder.append(this.extendedTunnelID);
		builder.append("]");
		return builder.toString();
	}

}
