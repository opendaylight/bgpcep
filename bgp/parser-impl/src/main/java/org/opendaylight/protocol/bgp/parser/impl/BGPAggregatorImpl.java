/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.concepts.BGPAggregator;

import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.NetworkAddress;

/**
 * Implementation of BGP Aggregator object.
 * @param <T> subtype of Network Address
 */
public class BGPAggregatorImpl<T extends NetworkAddress<T>> implements
		BGPAggregator {

	private final ASNumber asNumber;

	private final T address;

	public BGPAggregatorImpl(ASNumber asNumber, T address) {
		this.address = address;
		this.asNumber = asNumber;
	}

	@Override
	public ASNumber getASNumber() {
		return this.asNumber;
	}

	@Override
	public T getNetworkAddress() {
		return this.address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.address == null) ? 0 : this.address.hashCode());
		result = prime * result
				+ ((this.asNumber == null) ? 0 : this.asNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BGPAggregatorImpl<?> other = (BGPAggregatorImpl<?>) obj;
		if (this.address == null) {
			if (other.address != null)
				return false;
		} else if (!this.address.equals(other.address))
			return false;
		if (this.asNumber == null) {
			if (other.asNumber != null)
				return false;
		} else if (!this.asNumber.equals(other.asNumber))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPAggregatorImpl [asNumber=");
		builder.append(this.asNumber);
		builder.append(", address=");
		builder.append(this.address);
		builder.append("]");
		return builder.toString();
	}
}
