/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.NetworkAddress;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

public abstract class AbstractNetworkAddressRouterIdentifier<T extends NetworkAddress<?>> implements NetworkAddressRouterIdentifier<T> {
	private static final long serialVersionUID = 1L;
	private final T address;

	protected AbstractNetworkAddressRouterIdentifier(final T address) {
		Preconditions.checkNotNull(address, "Address may not be null");
		this.address = address;
	}

	@Override
	public T getAddress() {
		return this.address;
	}

	@Override
	public final String toString() {
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("address", this.address);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.address == null) ? 0 : this.address.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractNetworkAddressRouterIdentifier<?> other = (AbstractNetworkAddressRouterIdentifier<?>) obj;
		if (this.address == null) {
			if (other.address != null)
				return false;
		} else if (!this.address.equals(other.address))
			return false;
		return true;
	}
}
