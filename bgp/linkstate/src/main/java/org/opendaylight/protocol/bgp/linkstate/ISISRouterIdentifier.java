/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.ISOSystemIdentifier;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * An IS-IS node identifier.
 */
public final class ISISRouterIdentifier implements RouterIdentifier {
	private static final long serialVersionUID = 1L;
	private final ISOSystemIdentifier systemId;

	/**
	 * Construct a new node identifier. Formed as the unification of component identifiers.
	 * 
	 * @param systemId ISO System ID, may not be null
	 */
	public ISISRouterIdentifier(final ISOSystemIdentifier systemId) {
		this.systemId = Preconditions.checkNotNull(systemId, "ISO System Identifier is mandatory.");
	}

	public final ISOSystemIdentifier getSystemId() {
		return this.systemId;
	}

	@Override
	public final String toString() {
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("systemId", this.systemId);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.systemId == null) ? 0 : this.systemId.hashCode());
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
		if (this.getClass() != obj.getClass())
			return false;
		final ISISRouterIdentifier other = (ISISRouterIdentifier) obj;
		if (this.systemId == null) {
			if (other.systemId != null)
				return false;
		} else if (!this.systemId.equals(other.systemId))
			return false;
		return true;
	}
}
