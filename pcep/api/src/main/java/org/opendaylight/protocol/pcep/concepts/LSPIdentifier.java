/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;

/**
 * A 16-bit identifier used in the SENDER_TEMPLATE and the FILTER_SPEC that can
 * be changed to allow a sender to share resources with itself.
 */
public final class LSPIdentifier extends AbstractIdentifier<LSPIdentifier> {

	private static final long serialVersionUID = 1337756730239265010L;

	private final byte[] lspId;

	/**
	 * Creates LSPIdentifier using byte array as value.
	 *
	 * @param lspId
	 *            value of the LSPIdentifier TLV. Must be exactly 2 bytes long.
	 */
	public LSPIdentifier(final byte[] lspId) {
		if (lspId.length != 2)
			throw new IllegalArgumentException("Invalid LSP identifier");
		this.lspId = lspId;
	}

	/**
	 * Gets LSP Id in raw byte array representation.
	 *
	 * @return byte array representation of LSP ID. May be null.
	 */
	public byte[] getLspId() {
		return this.lspId;
	}

	@Override
	protected byte[] getBytes() {
		return this.lspId;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("lspId", ByteArray.toHexString(lspId, "."));
	}

}
