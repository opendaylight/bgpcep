/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP SVEC Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.13">PCEP SVEC Object</a>
 * @see <a href="http://tools.ietf.org/html/rfc6006#section-3.12">Synchronization of P2MP TE Path Computation Requests
 *      [RFC6006]</a>
 */
public class PCEPSvecObject extends PCEPObject {

	private final boolean linkDiversed;

	private final boolean nodeDiversed;

	private final boolean srlgDiversed;

	private final boolean paritialPathDiversed;

	private final boolean linkDirectionDiversed;

	private final List<Long> requestIDs;

	/**
	 * Constructs PCEP SVEC Object.
	 * 
	 * @param linkDiversed boolean
	 * @param nodeDiversed boolean
	 * @param srlgDiversed boolean
	 * @param requestIDs List<Long>. Can't be null or empty.
	 * @param processed boolean
	 */
	public PCEPSvecObject(final boolean linkDiversed, final boolean nodeDiversed, final boolean srlgDiversed,
			final boolean partialPathDiversed, final boolean linkDirectionDiversed, final List<Long> requestIDs, final boolean processed) {
		super(processed, false);
		this.linkDiversed = linkDiversed;
		this.nodeDiversed = nodeDiversed;
		this.srlgDiversed = srlgDiversed;
		this.paritialPathDiversed = partialPathDiversed;
		this.linkDirectionDiversed = linkDirectionDiversed;

		if (requestIDs == null || requestIDs.isEmpty())
			throw new IllegalArgumentException("RequestIDs can't be null or empty.");
		this.requestIDs = requestIDs;
	}

	/**
	 * Gets Link Diversed flag.
	 * 
	 * @return boolean
	 */
	public boolean isLinkDiversed() {
		return this.linkDiversed;
	}

	/**
	 * Gets Node Diversed flag.
	 * 
	 * @return boolean
	 */
	public boolean isNodeDiversed() {
		return this.nodeDiversed;
	}

	/**
	 * Gets Srlg Diversed flag.
	 * 
	 * @return boolean
	 */
	public boolean isSrlgDiversed() {
		return this.srlgDiversed;
	}

	public boolean isParitialPathDiversed() {
		return this.paritialPathDiversed;
	}

	public boolean isLinkDirectionDiversed() {
		return this.linkDirectionDiversed;
	}

	/**
	 * Gets list of Long representations of RequestIDs.
	 * 
	 * @return List<Long>. Can't be null or empty.
	 */
	public List<Long> getRequestIDs() {
		return this.requestIDs;
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
		result = prime * result + (this.linkDiversed ? 1231 : 1237);
		result = prime * result + (this.nodeDiversed ? 1231 : 1237);
		result = prime * result + ((this.requestIDs == null) ? 0 : this.requestIDs.hashCode());
		result = prime * result + (this.srlgDiversed ? 1231 : 1237);
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
		final PCEPSvecObject other = (PCEPSvecObject) obj;
		if (this.linkDiversed != other.linkDiversed)
			return false;
		if (this.nodeDiversed != other.nodeDiversed)
			return false;
		if (this.requestIDs == null) {
			if (other.requestIDs != null)
				return false;
		} else if (!this.requestIDs.equals(other.requestIDs))
			return false;
		if (this.srlgDiversed != other.srlgDiversed)
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("linkDiversed", this.linkDiversed);
		toStringHelper.add("nodeDiversed", this.nodeDiversed);
		toStringHelper.add("srlgDiversed", this.srlgDiversed);
		toStringHelper.add("paritialPathDiversed", this.paritialPathDiversed);
		toStringHelper.add("linkDirectionDiversed", this.linkDirectionDiversed);
		toStringHelper.add("requestIDs", this.requestIDs);
		return super.addToStringAttributes(toStringHelper);
	}
}
