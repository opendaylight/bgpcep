/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing a <a href="http://tools.ietf.org/html/rfc4360#section-3.3">Opaque Extended Community</a>.
 */
public class OpaqueExtendedCommunity extends ExtendedCommunity implements Serializable {

	private static final long serialVersionUID = 2675224758578219774L;
	private final byte[] value;
	private final int subType;

	/**
	 * Create a new opaque extended community.
	 * 
	 * @param transitive True if the community is transitive
	 * @param subType Community subtype, has to be in range 0-255
	 * @param value Community value, has to be 6 bytes long
	 * @throws IllegalArgumentException when either subtype or value are invalid
	 */
	public OpaqueExtendedCommunity(final boolean transitive, final int subType, final byte[] value) {
		super(false, transitive);
		Preconditions.checkArgument(subType > 0 && subType < 255, "Invalid Sub-Type");
		Preconditions.checkArgument(value.length == 6, "Invalid value");
		this.subType = subType;
		this.value = value;
	}

	/**
	 * Returns community subtype.
	 * 
	 * @return Community subtype
	 */
	public final int getSubType() {
		return this.subType;
	}

	/**
	 * Returns community value
	 * 
	 * @return Community value
	 */
	public final byte[] getValue() {
		return this.value;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("subType", this.subType);
		toStringHelper.add("value", this.value);
		return super.addToStringAttributes(toStringHelper);
	}
}
