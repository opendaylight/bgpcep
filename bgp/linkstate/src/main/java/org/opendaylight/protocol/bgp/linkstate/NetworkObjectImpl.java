/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Implementation of {@link NetworkObject}
 * 
 * @param <T> {@link Identifier} type
 */
public class NetworkObjectImpl<T extends Identifier> implements NetworkObject<T> {
	private static final long serialVersionUID = 1L;
	private final T name;
	protected NetworkObjectState state;

	/**
	 * 
	 * @param name
	 */
	public NetworkObjectImpl(final T name) {
		this(name, NetworkObjectState.EMPTY);
	}

	/**
	 * 
	 * @param name T
	 * @param template T
	 */
	public NetworkObjectImpl(final T name, final NetworkObjectState state) {
		Preconditions.checkNotNull(name);
		Preconditions.checkNotNull(state);
		this.name = name;
		this.state = state;
	}

	@Override
	public final T getName() {
		return this.name;
	}

	/**
	 * Standard setter for NetworkObject asPath attribute.
	 * 
	 * @param asPath {@link ASPath}
	 */
	public final synchronized void setASPath(final List<AsPathSegment> asPath) {
		this.state = this.state.withASPath(asPath);
	}

	/**
	 * Standard setter for NetworkObject communities attribute.
	 * 
	 * @param communities {@link CommunityImpl}
	 */
	public final synchronized void setCommunities(final Set<Community> communities) {
		this.state = this.state.withCommunities(Collections.unmodifiableSet(communities));
	}

	/**
	 * Standard setter for NetworkObject extendedCommunities attribute.
	 * 
	 * @param extendedCommunities {@link ExtendedCommunity}
	 */
	public final synchronized void setExtendedCommunities(final Set<ExtendedCommunity> extendedCommunities) {
		this.state = this.state.withExtendedCommunities(Collections.unmodifiableSet(extendedCommunities));
	}

	@Override
	public synchronized final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("name", this.name);
		toStringHelper.add("state", this.state);
		return toStringHelper;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (obj == null)
			return false;
		if (!(obj instanceof NetworkObjectImpl))
			return false;
		final NetworkObjectImpl<?> other = (NetworkObjectImpl<?>) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public synchronized NetworkObjectState currentState() {
		return this.state;
	}
}
