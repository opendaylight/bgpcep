/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * 
 * A common interface for objects that can be added to a topology. It can be either BGPRoute, BGPLink, BGPNode or
 * BGPPrefix.
 * 
 */
public class BaseBGPObjectState implements State {

	private static final long serialVersionUID = 2371691093534458869L;

	private final BgpOrigin origin;
	private final BGPAggregator aggregator;

	/**
	 * Creates BaswBGPObjectState.
	 * 
	 * @param origin {@link BGPOrigin}
	 * @param aggregator {@link BGPAggregator}
	 */
	public BaseBGPObjectState(final BgpOrigin origin, final BGPAggregator aggregator) {
		super();
		this.origin = origin;
		this.aggregator = aggregator;
	}

	protected BaseBGPObjectState(final BaseBGPObjectState orig) {
		super();
		this.origin = orig.origin;
		this.aggregator = orig.aggregator;
	}

	/**
	 * 
	 * @return the value of the ORIGIN attribute
	 */
	public final BgpOrigin getOrigin() {
		return this.origin;
	}

	/**
	 * 
	 * @return BGPAggregator attribute of this object
	 */
	public final BGPAggregator getAggregator() {
		return this.aggregator;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("aggregator", this.aggregator);
		toStringHelper.add("origin", this.origin);
		return toStringHelper;
	}

	protected BaseBGPObjectState newInstance() {
		return new BaseBGPObjectState(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.aggregator == null) ? 0 : this.aggregator.hashCode());
		result = prime * result + ((this.origin == null) ? 0 : this.origin.hashCode());
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
		if (getClass() != obj.getClass())
			return false;
		final BaseBGPObjectState other = (BaseBGPObjectState) obj;
		if (this.aggregator == null) {
			if (other.aggregator != null)
				return false;
		} else if (!this.aggregator.equals(other.aggregator))
			return false;
		if (this.origin != other.origin)
			return false;
		return true;
	}
}
