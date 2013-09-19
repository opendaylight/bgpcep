/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;

/**
 * Implementation of BGP Aggregator object. RFC4760 assumes that any BGP speaker (including the one that supports
 * multiprotocol capabilities defined in this document) has to have an IPv4 address (which will be used, among other
 * things, in the AGGREGATOR attribute).
 * 
 * @param <T> subtype of Network Address
 */
public class BGPAggregatorImpl implements BgpAggregator {

	private final AsNumber asNumber;

	private final Ipv4Address address;

	public BGPAggregatorImpl(final AsNumber asNumber, final Ipv4Address address) {
		this.address = address;
		this.asNumber = asNumber;
	}

	@Override
	public AsNumber getAsNumber() {
		return this.asNumber;
	}

	@Override
	public Ipv4Address getNetworkAddress() {
		return this.address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.address == null) ? 0 : this.address.hashCode());
		result = prime * result + ((this.asNumber == null) ? 0 : this.asNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final BGPAggregatorImpl other = (BGPAggregatorImpl) obj;
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
