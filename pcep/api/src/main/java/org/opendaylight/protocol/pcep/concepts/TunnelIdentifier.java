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
 * A 16-bit identifier used in the SESSION that remains constant over the life
 * of the tunnel.
 */
public final class TunnelIdentifier extends AbstractIdentifier<TunnelIdentifier> {

	private static final long serialVersionUID = 137237703900885441L;

	private final byte[] tunnelId;

	/**
	 * Creates TunnelIdentifier using byte array as value.
	 *
	 * @param tunnelId
	 *            value of the TunnelIdentifier TLV. Must be exactly 2 bytes
	 *            long.
	 */
	public TunnelIdentifier(final byte[] tunnelId) {
		if (tunnelId.length != 2)
			throw new IllegalArgumentException("Invalid tunnel ID.");
		this.tunnelId = tunnelId;
	}

	@Override
	public byte[] getBytes() {
		return this.tunnelId;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("tunnelId", ByteArray.toHexString(tunnelId, "."));
	}
}
