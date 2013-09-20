/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.parameter;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpSubsequentAddressFamily;

/**
 * Multiprotocol capability Parameter as described in:
 * 
 * <a href="http://tools.ietf.org/html/rfc4760#section-8">Use of BGP Capability Advertisement/a>
 */
public final class MultiprotocolCapability extends CapabilityParameter {

	/**
	 * Capability Code is set to 1, which indicates Multiprotocol Extensions capabilities.
	 */
	public static final int CODE = 1;

	private final BGPTableType tableType;

	/**
	 * Creates Multiprotocol Capability.
	 * 
	 * 
	 * @param type bgp table type
	 */
	public MultiprotocolCapability(final BGPTableType type) {
		super(CODE);
		this.tableType = type;
	}

	/**
	 * Returns numeric representation of AFI.
	 * 
	 * @return AFI
	 */
	public BgpAddressFamily getAfi() {
		return this.tableType.getAddressFamily();
	}

	/**
	 * Returns numeric representation of SAFI.
	 * 
	 * @return SAFI
	 */
	public BgpSubsequentAddressFamily getSafi() {
		return this.tableType.getSubsequentAddressFamily();
	}

	/**
	 * Returns BGP Table Type that is supported by the sender of this capability.
	 * 
	 * @return BGP Table Type
	 */
	public BGPTableType getTableType() {
		return this.tableType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("MultiprotocolCapability [tableType=");
		builder.append(this.tableType);
		builder.append("]");
		return builder.toString();
	}
}
