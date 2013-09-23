/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * DTO for BGP Session preferences, that contains BGP Open message.
 */
public final class BGPSessionPreferences {

	private final AsNumber as;

	private final int hold;

	private final IPv4Address bgpId;

	private final List<BGPParameter> params;

	/**
	 * Creates a new DTO for Open message.
	 * 
	 * @param prefs BGP Open message
	 */
	public BGPSessionPreferences(final AsNumber as, final int hold, final IPv4Address bgpId, final List<BGPParameter> params) {
		this.as = as;
		this.hold = hold;
		this.bgpId = bgpId;
		this.params = params;
	}

	/**
	 * Returns my AS number.
	 * 
	 * @return AS number
	 */
	public AsNumber getMyAs() {
		return this.as;
	}

	/**
	 * Returns initial value of HoldTimer.
	 * 
	 * @return initial value of HoldTimer
	 */
	public int getHoldTime() {
		return this.hold;
	}

	/**
	 * Returns my BGP Identifier.
	 * 
	 * @return BGP identifier
	 */
	public IPv4Address getBgpId() {
		return this.bgpId;
	}

	public List<BGPParameter> getParams() {
		return this.params;
	}
}
