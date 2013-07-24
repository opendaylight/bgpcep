/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Arrays;

import org.opendaylight.protocol.concepts.AbstractIdentifier;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Domain Identifier SubTLV from Identifier TLV.
 * 
 * @see <a href="http://tools.ietf.org/html/draft-ietf-idr-ls-distribution-02#section-3.2.1.2">Domain Identifier
 *      SubTLV</a>
 */
public final class DomainIdentifier extends AbstractIdentifier<DomainIdentifier> {

	private static final long serialVersionUID = -5033319198070873474L;

	private final byte[] id;

	/**
	 * Create a new domain identifier using its raw identifier. There is not length constraint.
	 * 
	 * @param id Raw identifier, has to be four bytes long.
	 */
	public DomainIdentifier(final byte[] id) {
		Preconditions.checkNotNull(id);
		Preconditions.checkArgument(id.length == 4);
		this.id = id;
	}

	/**
	 * Returns the raw identifier
	 * 
	 * @return Raw identifier
	 */
	@Override
	public byte[] getBytes() {
		return this.id;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("id", ByteArray.toHexString(id, "."));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(this.id);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DomainIdentifier other = (DomainIdentifier) obj;
		if (!Arrays.equals(this.id, other.id))
			return false;
		return true;
	}
}
