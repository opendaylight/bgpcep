/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * @see https://tools.ietf.org/html/draft-ietf-idr-ls-distribution-03#section-3.3.1.2
 */
public final class ISISAreaIdentifier extends AbstractIdentifier<ISISAreaIdentifier> {
	private static final long serialVersionUID = 1L;
	private final byte[] id;

	/**
	 * Construct a new Node Area identifier. This constructor expects
	 * a non-empty byte array which is the are distinguisher. Maximum
	 * length of the array is capped at 20 bytes.
	 *
	 * @param id Area identifier, expressed as a byte array
	 * @throws IllegalArgumentException if the identifier is not valid
	 */
	public ISISAreaIdentifier(final byte[] id) {
		this.id = Preconditions.checkNotNull(id);
		Preconditions.checkArgument(id.length > 0 && id.length <= 20);
	}

	/**
	 * Access the underlying identifier.
	 * 
	 * @return Identifier bytearray of AreaIdentifier.
	 */
	@Override
	public final byte[] getBytes() {
		return this.id;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("id", ByteArray.toHexString(id, "."));
	}
}

