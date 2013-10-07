/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure for PCEP Load Balancing Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.16">PCEP Load Balancing Object</a>
 */
public class PCEPLoadBalancingObject extends PCEPObject {

	private final int maxLSP;

	private final Bandwidth minBandwidth;

	/**
	 * Constructs Load Balancing Object.
	 * 
	 * @param maxLSP int
	 * @param minBandwidth Bandwidth
	 * @param processed boolean
	 */
	public PCEPLoadBalancingObject(final int maxLSP, final Bandwidth minBandwidth, final boolean processed) {
		super(processed, false);
		this.maxLSP = maxLSP;
		this.minBandwidth = minBandwidth;
	}

	/**
	 * Gets Maximum LSP.
	 * 
	 * @return int
	 */
	public int getMaxLSP() {
		return this.maxLSP;
	}

	/**
	 * Gets Minimal bandwidth.
	 * 
	 * @return Bandwidth
	 */
	public Bandwidth getMinBandwidth() {
		return this.minBandwidth;
	}

	@Override
	public Boolean isIgnore() {
		return super.isIgnored();
	}

	@Override
	public Boolean isProcessingRule() {
		return super.isProcessed();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.maxLSP;
		result = prime * result + ((this.minBandwidth == null) ? 0 : this.minBandwidth.hashCode());
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
		final PCEPLoadBalancingObject other = (PCEPLoadBalancingObject) obj;
		if (this.maxLSP != other.maxLSP)
			return false;
		if (this.minBandwidth == null) {
			if (other.minBandwidth != null)
				return false;
		} else if (!this.minBandwidth.equals(other.minBandwidth))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("maxLSP", this.maxLSP);
		toStringHelper.add("minBandwidth", this.minBandwidth);
		return super.addToStringAttributes(toStringHelper);
	}
}
