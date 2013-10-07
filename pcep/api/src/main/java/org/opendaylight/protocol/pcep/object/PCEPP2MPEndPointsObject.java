/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.concepts.NetworkAddress;

import com.google.common.base.Objects.ToStringHelper;

public class PCEPP2MPEndPointsObject<T extends NetworkAddress<T>> extends PCEPEndPoints {

	private final long leafType;

	private final T sourceAddress;

	private final List<T> destinationAddresses;

	public PCEPP2MPEndPointsObject(final long leafType, final T sourceAddress, final List<T> destinationAddresses, final boolean processed,
			final boolean ignored) {
		super(processed, ignored);
		this.leafType = leafType;
		this.sourceAddress = sourceAddress;
		if (destinationAddresses == null || destinationAddresses.isEmpty())
			throw new IllegalArgumentException("At least one destination have to be specified.");

		this.destinationAddresses = destinationAddresses;
	}

	/**
	 * @return the leafType
	 */
	public long getLeafType() {
		return this.leafType;
	}

	/**
	 * @return the sourceAddress
	 */
	public T getSourceAddress() {
		return this.sourceAddress;
	}

	/**
	 * @return the destinationAddresses
	 */
	public List<T> getDestinationAddresses() {
		return this.destinationAddresses;
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
		result = prime * result + ((this.destinationAddresses == null) ? 0 : this.destinationAddresses.hashCode());
		result = prime * result + (int) (this.leafType ^ (this.leafType >>> 32));
		result = prime * result + ((this.sourceAddress == null) ? 0 : this.sourceAddress.hashCode());
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
		final PCEPP2MPEndPointsObject<?> other = (PCEPP2MPEndPointsObject<?>) obj;
		if (this.destinationAddresses == null) {
			if (other.destinationAddresses != null)
				return false;
		} else if (!this.destinationAddresses.equals(other.destinationAddresses))
			return false;
		if (this.leafType != other.leafType)
			return false;
		if (this.sourceAddress == null) {
			if (other.sourceAddress != null)
				return false;
		} else if (!this.sourceAddress.equals(other.sourceAddress))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("leafType", this.leafType);
		toStringHelper.add("sourceAddress", this.sourceAddress);
		toStringHelper.add("destinationAddresses", this.destinationAddresses);
		return super.addToStringAttributes(toStringHelper);
	}

}
