/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.message;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;

import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4Address;

/**
 * Representation of BGPOpen message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.2">BGP Open Message</a>
 */
public final class BGPOpenMessage implements BGPMessage {

	private static final long serialVersionUID = -3507481736478860117L;

	/**
	 * Current BGP version.
	 */
	public static final int BGP_VERSION = 4;

	private final ASNumber myAS;

	private final short holdTime;

	private final IPv4Address bgpId;

	private final List<BGPParameter> optParams;

	/**
	 * Creates BGPOpen message.
	 * 
	 * @param myAS ASNumber of the BGP speaker
	 * @param holdTime proposed value of the Hold Timer
	 * @param bgpId IPv4 Address of the BGP speaker
	 * @param optParams List of optional parameters
	 */
	public BGPOpenMessage(final ASNumber myAS, final short holdTime, final IPv4Address bgpId, final List<BGPParameter> optParams) {
		super();
		this.myAS = myAS;
		this.holdTime = holdTime;
		this.bgpId = bgpId;
		this.optParams = optParams;
	}

	/**
	 * Returns the AS number of the BGP speaker.
	 * 
	 * @return myAS
	 */
	public ASNumber getMyAS() {
		return this.myAS;
	}

	/**
	 * Returns the value of the Hold timer.
	 * 
	 * @return the holdTime
	 */
	public short getHoldTime() {
		return this.holdTime;
	}

	/**
	 * Returns BGP identifier of the speaker (his IP address).
	 * 
	 * @return the bgpId
	 */
	public IPv4Address getBgpId() {
		return this.bgpId;
	}

	/**
	 * Returns optional parameters in form of TLVs.
	 * 
	 * @return the optParams
	 */
	public List<BGPParameter> getOptParams() {
		return this.optParams;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPOpenMessage [myAS=");
		builder.append(this.myAS);
		builder.append(", holdTime=");
		builder.append(this.holdTime);
		builder.append(", bgpId=");
		builder.append(this.bgpId);
		builder.append(", optParams=");
		builder.append(this.optParams);
		builder.append("]");
		return builder.toString();
	}
}
