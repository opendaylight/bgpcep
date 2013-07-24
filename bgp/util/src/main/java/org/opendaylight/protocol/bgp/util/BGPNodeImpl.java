/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BGPAggregator;
import org.opendaylight.protocol.bgp.concepts.BGPOrigin;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.parser.BGPNode;
import org.opendaylight.protocol.bgp.parser.BGPNodeState;

import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NetworkNode;
import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;
import com.google.common.base.Objects.ToStringHelper;

/**
 * 
 * BGP Link-State Node object is a composition object of BGPObject (so it can be contained in addedObjects in Update
 * Message) and Network Node, that merges the attributes from Link-State NLRI about Nodes.
 * 
 */
public final class BGPNodeImpl extends AbstractBGPObject implements BGPNode {
	private static final long serialVersionUID = 1L;

	private final NodeIdentifier nodeIdentifier;

	/**
	 * Creates this object with given arguments.
	 * 
	 * @param origin {@link BGPOrigin}
	 * @param aggregator {@link BGPAggregator}
	 * @param nodeIdentifier {@link NodeIdentifier}
	 * @param nodeAttributes {@link NetworkNode}
	 */
	public BGPNodeImpl(final BaseBGPObjectState base, final NodeIdentifier nodeIdentifier, final NetworkNodeState nodeState) {
		super(new BGPNodeState(base, nodeState));
		this.nodeIdentifier = nodeIdentifier;
	}

	@Override
	public BGPNodeState currentState() {
		return (BGPNodeState) super.currentState();
	}

	@Override
	public NodeIdentifier getNodeIdentifier() {
		return this.nodeIdentifier;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("nodeDescriptor", this.nodeIdentifier);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.nodeIdentifier == null) ? 0 : this.nodeIdentifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BGPNodeImpl other = (BGPNodeImpl) obj;
		if (this.nodeIdentifier == null) {
			if (other.nodeIdentifier != null)
				return false;
		} else if (!this.nodeIdentifier.equals(other.nodeIdentifier))
			return false;
		return true;
	}
}
