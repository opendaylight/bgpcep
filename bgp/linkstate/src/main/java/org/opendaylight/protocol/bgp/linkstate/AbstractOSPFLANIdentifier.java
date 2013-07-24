/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * An abstract OSPF LAN "pseudonode" identifier. This class is specialized for OSPFv2 and OSPFv3 as the two differ only
 * slightly in semantics.
 */
public abstract class AbstractOSPFLANIdentifier<T extends InterfaceIdentifier> extends AbstractLANIdentifier<OSPFRouterIdentifier> {
	private static final long serialVersionUID = 1L;
	private final T lanInterface;

	protected AbstractOSPFLANIdentifier(final OSPFRouterIdentifier dr, final T lanInterface) {
		super(dr);
		this.lanInterface = Preconditions.checkNotNull(lanInterface);
	}

	public final T getLANInterface() {
		return this.lanInterface;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("lanInterface", this.lanInterface);
		return super.addToStringAttributes(toStringHelper);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((lanInterface == null) ? 0 : lanInterface.hashCode());
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
		AbstractOSPFLANIdentifier<?> other = (AbstractOSPFLANIdentifier<?>) obj;
		if (lanInterface == null) {
			if (other.lanInterface != null)
				return false;
		} else if (!lanInterface.equals(other.lanInterface))
			return false;
		return true;
	}
}
