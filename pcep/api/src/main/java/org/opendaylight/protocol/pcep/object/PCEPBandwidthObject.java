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
 * Basic structure of Bandwidth Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.7">PCEP Bandwidth Object</a>
 */
public abstract class PCEPBandwidthObject extends PCEPObject {

	private final Bandwidth bandwidth;

	/**
	 * Constructs basic Bandwidth Object.
	 * 
	 * @param bandwidth Bandwidth
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPBandwidthObject(final Bandwidth bandwidth, final boolean processed, final boolean ignored) {
		super(processed, ignored);
		if (bandwidth == null)
			this.bandwidth = new Bandwidth(new byte[] { 0 });
		else
			this.bandwidth = bandwidth;
	}

	/**
	 * Gets {@link PCEPBandwidthObject}.
	 * 
	 * @return Bandwidth. Can't be null.
	 */
	public Bandwidth getBandwidth() {
		return this.bandwidth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.bandwidth == null) ? 0 : this.bandwidth.hashCode());
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
		final PCEPBandwidthObject other = (PCEPBandwidthObject) obj;
		if (this.bandwidth == null) {
			if (other.bandwidth != null)
				return false;
		} else if (!this.bandwidth.equals(other.bandwidth))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("bandwidth", this.bandwidth);
		return super.addToStringAttributes(toStringHelper);
	}
}
