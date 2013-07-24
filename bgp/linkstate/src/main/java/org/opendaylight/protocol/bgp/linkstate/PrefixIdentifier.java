/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.concepts.Prefix;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * A network node identifier. A network node is typically a router, a switch, or similar entity.
 */
public class PrefixIdentifier<T extends NetworkAddress<?>> implements Identifier {
	private static final long serialVersionUID = 1L;
	private final NodeIdentifier owner;
	private final Prefix<T> prefix;

	public PrefixIdentifier(final NodeIdentifier owner, final Prefix<T> prefix) {
		this.owner = Preconditions.checkNotNull(owner);
		this.prefix = Preconditions.checkNotNull(prefix);
	}

	public final NodeIdentifier getOwner() {
		return owner;
	}

	/**
	 * Return the Prefix.
	 * 
	 * @return prefix
	 */
	public final Prefix<T> getPrefix() {
		return prefix;
	}

	@Override
	public final String toString(){
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("owner", owner);
		toStringHelper.add("prefix", prefix);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrefixIdentifier<?> other = (PrefixIdentifier<?>) obj;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		return true;
	}
}

