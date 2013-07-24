/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Arrays;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * An OSPF node identifier.
 */
public final class OSPFRouterIdentifier implements RouterIdentifier {
	private static final long serialVersionUID = 1L;
	private final byte[] routerId;

	/**
	 * Construct a new node identifier. Formed as the unification of component identifiers.
	 * 
	 * @param routerId OSPF Router ID
	 */
	public OSPFRouterIdentifier(final byte[] routerId) {
		Preconditions.checkNotNull(routerId, "Router identifier may not be null");
		// @see https://tools.ietf.org/html/draft-ietf-idr-ls-distribution-03#section-3.2.1.4
		Preconditions.checkArgument(routerId.length == 4, "Router identifier must be 4 bytes long");
		this.routerId = routerId;
	}

	public byte[] getRouterId() {
		return this.routerId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.routerId);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final OSPFRouterIdentifier other = (OSPFRouterIdentifier) obj;
		if (!Arrays.equals(this.routerId, other.routerId))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("routerID", Arrays.toString(this.routerId));
	}
}
