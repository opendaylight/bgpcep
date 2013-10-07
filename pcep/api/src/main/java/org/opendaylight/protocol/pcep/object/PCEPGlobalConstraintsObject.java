/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;

import com.google.common.base.Objects.ToStringHelper;

/**
 * The GLOBAL CONSTRAINTS Object is used in a PCReq message to specify the necessary global constraints that should be
 * applied to all individual path computations for a global concurrent path optimization request.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5557#section-5.5">GLOBAL CONSTRAINTS (GC) Object</a>
 */
public class PCEPGlobalConstraintsObject extends PCEPObject {

	private final short maxHop;
	private final short maxUtilization;
	private final short minUtilization;
	private final short overBookingFactor;

	private List<PCEPTlv> tlvs;

	/**
	 * Constructs Global Constraints Object with all mandatory fields.
	 * 
	 * @param maxhop 8-bit integer
	 * @param maxUtilization 8-bit integer between 0 and 100
	 * @param minUtilization 8-bit integer between 0 and 100
	 * @param overBookingFactor 8-bit integer
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPGlobalConstraintsObject(final short maxhop, final short maxUtilization, final short minUtilization,
			final short overBookingFactor, final boolean processed, final boolean ignored) {
		this(maxhop, maxUtilization, minUtilization, overBookingFactor, null, processed, ignored);
	}

	/**
	 * Constructs Global Constraints Object with all mandatory fields and optional list of tlvs.
	 * 
	 * @param maxhop 8-bit integer
	 * @param maxUtilization 8-bit integer between 0 and 100
	 * @param minUtilization 8-bit integer between 0 and 100
	 * @param overBookingFactor 8-bit integer
	 * @param tlvs list of tlvs
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPGlobalConstraintsObject(final short maxhop, final short maxUtilization, final short minUtilization,
			final short overBookingFactor, final List<PCEPTlv> tlvs, final boolean processed, final boolean ignored) {
		super(processed, ignored);
		this.maxHop = maxhop;

		if (maxUtilization < 0 || maxUtilization > 100)
			throw new IllegalArgumentException("Maximu utilization can be only between 0 and 100. Passed: " + maxUtilization);
		this.maxUtilization = maxUtilization;

		if (minUtilization < 0 || minUtilization > 100)
			throw new IllegalArgumentException("Minimum utilization can be only between 0 and 100. Passed: " + minUtilization);
		this.minUtilization = minUtilization;

		this.overBookingFactor = overBookingFactor;

		if (tlvs == null)
			this.tlvs = Collections.emptyList();
		else
			this.tlvs = tlvs;
	}

	/**
	 * Gets the maximum hop count for all the TE LSPs
	 * 
	 * @return the maximum hop count
	 */
	public short getMaxHop() {
		return this.maxHop;
	}

	/**
	 * Gets the maximum utilization percentage by which all links should be bound
	 * 
	 * @return the maximum utilization percentage
	 */
	public short getMaxUtilization() {
		return this.maxUtilization;
	}

	/**
	 * Gets the minimum utilization percentage by which all links should be bound
	 * 
	 * @return the maximum utilization percentage
	 */
	public short getMinUtilization() {
		return this.minUtilization;
	}

	/**
	 * Gets the over booking factor percentage that allows the reserved bandwidth to be overbooked on each link beyond
	 * its physical capacity limit
	 * 
	 * @return the over booking factor percentage
	 */
	public short getOverBookingFactor() {
		return this.overBookingFactor;
	}

	/**
	 * Gets the list of optional tlvs
	 * 
	 * @return List<PCEPTlv>. Can be empty but not null.
	 */
	public List<PCEPTlv> getTlvs() {
		return this.tlvs;
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
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("maxHop", this.maxHop);
		toStringHelper.add("maxUtilization", this.maxUtilization);
		toStringHelper.add("minUtilization", this.minUtilization);
		toStringHelper.add("overBookingFactor", this.overBookingFactor);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.maxHop;
		result = prime * result + this.maxUtilization;
		result = prime * result + this.minUtilization;
		result = prime * result + this.overBookingFactor;
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
		final PCEPGlobalConstraintsObject other = (PCEPGlobalConstraintsObject) obj;
		if (this.maxHop != other.maxHop)
			return false;
		if (this.maxUtilization != other.maxUtilization)
			return false;
		if (this.minUtilization != other.minUtilization)
			return false;
		if (this.overBookingFactor != other.overBookingFactor)
			return false;
		return true;
	}
}
