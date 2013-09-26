/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.bgp.parser.BGPRoute;
import org.opendaylight.protocol.bgp.parser.BGPRouteState;
import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.concepts.Prefix;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Super class for BGPIpvXPrefixImpl.
 * 
 * @param <T> extends NetworkAddress<T>
 */
public abstract class AbstractBGPRoute<T extends NetworkAddress<T>> extends AbstractBGPObject implements BGPRoute {
	private static final long serialVersionUID = 1L;
	private final Prefix<T> name;

	protected AbstractBGPRoute(final Prefix<T> name, final BaseBGPObjectState base, final NetworkRouteState routeState) {
		super(new BGPRouteState(base, routeState));
		this.name = Preconditions.checkNotNull(name);
	}

	@Override
	final public BGPRouteState currentState() {
		return (BGPRouteState) super.currentState();
	}

	@Override
	final public Prefix<T> getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
		final AbstractBGPRoute<?> other = (AbstractBGPRoute<?>) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("name", this.name);
		return super.addToStringAttributes(toStringHelper);
	}
}
