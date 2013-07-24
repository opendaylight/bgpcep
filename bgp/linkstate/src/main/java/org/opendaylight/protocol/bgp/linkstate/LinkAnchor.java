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
 * Identifier specifying a Link Anchor. A link is anchored by two nodes.
 * In case there are multiple links between two nodes with the same
 * directionality, each of them carries a different Interface Identifier.
 */
public final class LinkAnchor implements Identifier {

	private static final long serialVersionUID = 4768211569568229262L;

	private final NodeIdentifier nodeIdentifier;

	private final InterfaceIdentifier interfaceIdentifier;

	/**
	 * Construct a new link anchor.
	 *
	 * @param nodeIdentifier Node Identifier part, may not be null
	 * @param interfaceIdentifier Interface Identifier part, possibly null
	 */
	public LinkAnchor(final NodeIdentifier nodeIdentifier, final InterfaceIdentifier interfaceIdentifier) {
		if(nodeIdentifier == null)
			throw new NullPointerException("Can not create link anchor with null node identifier.");
		this.nodeIdentifier = nodeIdentifier;
		this.interfaceIdentifier = interfaceIdentifier;
	}

	/**
	 * Returns the Node Identifier part of this anchor.
	 *
	 * @return Node Identifier part
	 */
	public NodeIdentifier getNodeIdentifier() {
		return this.nodeIdentifier;
	}

	/**
	 * Returns the Interface Identifier part of this anchor.
	 *
	 * @return Interface Identifier part, may be null
	 */
	public InterfaceIdentifier getInterfaceIdentifier() {
		return this.interfaceIdentifier;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("node", nodeIdentifier);
		toStringHelper.add("interface", interfaceIdentifier);
		return toStringHelper;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ (this.interfaceIdentifier == null ? 0 : this.interfaceIdentifier
						.hashCode());
		result = prime * result
				+ (this.nodeIdentifier == null ? 0 : this.nodeIdentifier.hashCode());
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
		final LinkAnchor other = (LinkAnchor) obj;
		if (this.interfaceIdentifier == null) {
			if (other.interfaceIdentifier != null)
				return false;
		} else if (!this.interfaceIdentifier.equals(other.interfaceIdentifier))
			return false;
		if (this.nodeIdentifier == null) {
			if (other.nodeIdentifier != null)
				return false;
		} else if (!this.nodeIdentifier.equals(other.nodeIdentifier))
			return false;
		return true;
	}
}
