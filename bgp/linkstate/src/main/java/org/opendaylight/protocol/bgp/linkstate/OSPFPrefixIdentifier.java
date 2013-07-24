/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.concepts.Prefix;
import com.google.common.base.Objects.ToStringHelper;

/**
 * A network node identifier. A network node is typically a router, a switch, or similar entity.
 */
public final class OSPFPrefixIdentifier<T extends NetworkAddress<?>> extends PrefixIdentifier<T> {
	private static final long serialVersionUID = 1L;
	private final OSPFRouteType routeType;

	public OSPFPrefixIdentifier(final NodeIdentifier owner, final Prefix<T> prefix, final OSPFRouteType routeType) {
		super(owner, prefix);
		this.routeType = routeType;
	}

	/**
	 * Returns OSPF Route Type.
	 * 
	 * @return {@link OSPFRouteType}
	 */
	public OSPFRouteType getRouteType() {
		return routeType;
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("routeType", routeType);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((routeType == null) ? 0 : routeType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSPFPrefixIdentifier<?> other = (OSPFPrefixIdentifier<?>) obj;
		if (routeType != other.routeType)
			return false;
		return true;
	}
}

