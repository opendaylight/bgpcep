/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.parser.BGPLink;
import org.opendaylight.protocol.bgp.parser.BGPLinkState;

import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * 
 * BGP Link-State Link object is a composition object of BGPObject (so it can be contained in addedObjects in Update
 * Message) and Network Link, that merges the attributes from Link-State NLRI about Links.
 * 
 */
public final class BGPLinkImpl extends AbstractBGPObject implements BGPLink {

	private static final long serialVersionUID = -226135392855622720L;

	private final LinkIdentifier linkIdentifier;

	/**
	 * Creates a new Link with given arguments.
	 * 
	 * @param origin BGPOrigin
	 * @param aggregator BGPAggregator
	 * @param linkIdentifier LinkIdentifier
	 * @param linkAttributes NetworkLink
	 */
	public BGPLinkImpl(final BaseBGPObjectState base, final LinkIdentifier linkIdentifier, final NetworkLinkState linkState) {
		super(new BGPLinkState(base, linkState));
		Preconditions.checkNotNull(linkIdentifier);
		this.linkIdentifier = linkIdentifier;
	}

	@Override
	public LinkIdentifier getLinkIdentifier() {
		return this.linkIdentifier;
	}

	@Override
	public BGPLinkState currentState() {
		return (BGPLinkState) super.currentState();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.linkIdentifier == null) ? 0 : this.linkIdentifier.hashCode());
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
		if (!(obj instanceof BGPLinkImpl))
			return false;
		final BGPLinkImpl other = (BGPLinkImpl) obj;
		if (this.linkIdentifier == null) {
			if (other.linkIdentifier != null)
				return false;
		} else if (!this.linkIdentifier.equals(other.linkIdentifier))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("linkIdentifier", this.linkIdentifier);
		return super.addToStringAttributes(toStringHelper);
	}
}
