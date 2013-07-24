/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.Identifier;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * A network link identifier. An instance of this object uniquely identifies
 * a link between two nodes.
 */
public final class LinkIdentifier implements Identifier {
	private static final long serialVersionUID = 1L;
	private final TopologyIdentifier topology;
	private final LinkAnchor localAnchor, remoteAnchor;

	/**
	 * Construct a new Link identifier based on its partial identifiers.
	 *
	 * @param topology Link topology identifier, may be null
	 * @param localAnchor Link local anchor
	 * @param remoteAnchor Link remote anchor
	 * @throws IllegalArgumentException when anchors do not have equal
	 *         source protocol
	 */
	public LinkIdentifier(final TopologyIdentifier topology, final LinkAnchor localAnchor, final LinkAnchor remoteAnchor) {
		this.topology = topology;
		this.localAnchor = localAnchor;
		this.remoteAnchor = remoteAnchor;
	}

	/**
	 * Returns the local anchor identifier.
	 *
	 * @return Local anchor identifier
	 */
	public LinkAnchor getLocalAnchor() {
		return localAnchor;
	}

	/**
	 * Returns the remote anchor identifier.
	 *
	 * @return Remove anchor identifier
	 */
	public LinkAnchor getRemoteAnchor() {
		return remoteAnchor;
	}

	/**
	 * Returns identifier of topology to which this link belongs.
	 *
	 * @return Link's topology identifier
	 */
	public TopologyIdentifier getTopology() {
		return topology;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("topology", topology);
		toStringHelper.add("localAnchor", localAnchor);
		toStringHelper.add("remoteAnchor", remoteAnchor);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (topology == null ? 0 : topology.hashCode());
		result = prime * result + (localAnchor == null ? 0 : localAnchor.hashCode());
		result = prime * result + (remoteAnchor == null ? 0 : remoteAnchor.hashCode());
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
		final LinkIdentifier other = (LinkIdentifier) obj;
		if (localAnchor == null) {
			if (other.localAnchor != null)
				return false;
		} else if (!localAnchor.equals(other.localAnchor))
			return false;
		if (remoteAnchor == null) {
			if (other.remoteAnchor != null)
				return false;
		} else if (!remoteAnchor.equals(other.remoteAnchor))
			return false;
		if (topology == null) {
			if (other.topology != null)
				return false;
		} else if (!topology.equals(other.topology))
			return false;
		return true;
	}
}

