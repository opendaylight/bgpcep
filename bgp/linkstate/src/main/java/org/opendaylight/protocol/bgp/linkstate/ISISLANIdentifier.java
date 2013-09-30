/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IsoSystemIdentifier;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * An IS-IS node identifier.
 */
public final class ISISLANIdentifier extends AbstractLANIdentifier<ISISRouterIdentifier> {
	private static final long serialVersionUID = 1L;
	private final short psn;

	/**
	 * Construct a new node identifier. Formed as the unification of component identifiers.
	 * 
	 * @param systemId ISO System ID, may not be null
	 */
	public ISISLANIdentifier(final IsoSystemIdentifier systemId, final short psn) {
		this(new ISISRouterIdentifier(systemId), psn);
	}

	public ISISLANIdentifier(final ISISRouterIdentifier dis, final short psn) {
		super(dis);
		Preconditions.checkArgument(psn > 0 && psn < 255);
		this.psn = psn;
	}

	public short getPSN() {
		return this.psn;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("psn", this.psn);
		return super.addToStringAttributes(toStringHelper);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.psn;
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
		final ISISLANIdentifier other = (ISISLANIdentifier) obj;
		if (this.psn != other.psn)
			return false;
		return true;
	}
}
