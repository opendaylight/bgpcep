/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Object representing an ISO System identifier. It is the 6-byte system ID portion of the IS-IS network service access
 * point (NSAP).
 */
public final class ISOSystemIdentifier extends AbstractIdentifier<ISOSystemIdentifier> {
	private static final long serialVersionUID = 4493531385611530076L;
	private final byte[] systemId;

	/**
	 * Initialize a new ISO system identifier.
	 * 
	 * @param systemId 6-byte identifier
	 * @throws
	 * @li IllegalArgumentException if the length of supplied systemId is not 6 bytes
	 */
	public ISOSystemIdentifier(final byte[] systemId) {
		if (systemId.length != 6)
			throw new IllegalArgumentException("Invalid ISO System ID");
		this.systemId = systemId;
	}

	@Override
	public byte[] getBytes() {
		return this.systemId;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("id", ByteArray.toHexString(this.systemId, "."));
	}
}
