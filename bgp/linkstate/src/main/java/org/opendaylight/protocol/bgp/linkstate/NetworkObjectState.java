/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.ASPath;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.concepts.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Class representing state of a generic object living somewhere in the network. Each such object has potentially
 * arrived through BGP-like inter-domain distribution channel and thus contains equivalent tags -- which may be empty.
 */
public class NetworkObjectState implements State {
	public static final NetworkObjectState EMPTY = new NetworkObjectState();
	private static final long serialVersionUID = 1L;
	private Set<ExtendedCommunity> extendedCommunities;
	private Set<Community> communities;
	private ASPath asPath;

	protected NetworkObjectState() {
		this(ASPath.EMPTY, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());
	}

	public NetworkObjectState(final ASPath asPath, final Set<Community> communities, final Set<ExtendedCommunity> extendedCommunities) {
		this.asPath = asPath;
		this.communities = communities;
		this.extendedCommunities = extendedCommunities;
	}

	protected NetworkObjectState(final NetworkObjectState orig) {
		this.asPath = orig.asPath;
		this.communities = orig.communities;
		this.extendedCommunities = orig.extendedCommunities;
	}

	/**
	 * Get the AS path from the local network to the object. This may be null if the installing source has no idea about
	 * ASes, as would be the case of network nodes learned from an IGP.
	 * 
	 * @return Path to the advertising Autonomous System.
	 */
	public final ASPath getASPath() {
		return this.asPath;
	}

	public final NetworkObjectState withASPath(final ASPath asPath) {
		final NetworkObjectState ret = newInstance();
		ret.asPath = asPath;
		return ret;
	}

	/**
	 * Get BGP communities associated with this object. If this object has no RFC1997 extended communities attached, the
	 * returned set will be empty.
	 * 
	 * @return A set of RFC1997 communities.
	 */
	public final Set<Community> getCommunities() {
		return this.communities;
	}

	public final NetworkObjectState withCommunities(final Set<Community> communities) {
		final NetworkObjectState ret = newInstance();
		ret.communities = communities;
		return ret;
	}

	/**
	 * Get BGP communities associated with this object. If this object has no RFC4360 extended communities attached, the
	 * returned set will be empty.
	 * 
	 * @return A set of RFC4360 extended communities.
	 */
	public final Set<ExtendedCommunity> getExtendedCommunities() {
		return this.extendedCommunities;
	}

	public final NetworkObjectState withExtendedCommunities(final Set<ExtendedCommunity> extendedCommunities) {
		final NetworkObjectState ret = newInstance();
		ret.extendedCommunities = extendedCommunities;
		return ret;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("ASPath", this.asPath);
		toStringHelper.add("communities", this.communities);
		toStringHelper.add("extendedCommunities", this.extendedCommunities);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.asPath == null) ? 0 : this.asPath.hashCode());
		result = prime * result + ((this.communities == null) ? 0 : this.communities.hashCode());
		result = prime * result + ((this.extendedCommunities == null) ? 0 : this.extendedCommunities.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final NetworkObjectState other = (NetworkObjectState) obj;
		if (this.asPath == null) {
			if (other.asPath != null)
				return false;
		} else if (!this.asPath.equals(other.asPath))
			return false;
		if (this.communities == null) {
			if (other.communities != null)
				return false;
		} else if (!this.communities.equals(other.communities))
			return false;
		if (this.extendedCommunities == null) {
			if (other.extendedCommunities != null)
				return false;
		} else if (!this.extendedCommunities.equals(other.extendedCommunities))
			return false;
		return true;
	}

	protected NetworkObjectState newInstance() {
		return new NetworkObjectState(this);
	}
}
