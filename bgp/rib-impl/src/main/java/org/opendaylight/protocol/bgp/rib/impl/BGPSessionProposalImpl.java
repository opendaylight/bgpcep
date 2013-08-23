/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.List;

import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.parameter.AS4BytesCapability;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4Address;

import com.google.common.collect.Lists;

/**
 * Basic implementation of BGP Session Proposal. The values are taken from conf-bgp.
 */
public final class BGPSessionProposalImpl implements BGPSessionProposal {

	private final short holdTimer;

	private final ASNumber as;

	private final IPv4Address bgpId;

	private final BGPSessionPreferences prefs;

	public BGPSessionProposalImpl(final short holdTimer, final ASNumber as, final IPv4Address bgpId) {
		this.holdTimer = holdTimer;
		this.as = as;
		this.bgpId = bgpId;

		final BGPTableType ipv4 = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);
		final BGPTableType linkstate = new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Linkstate);
		final List<BGPParameter> tlvs = Lists.newArrayList();
		tlvs.add(new MultiprotocolCapability(ipv4));
		tlvs.add(new MultiprotocolCapability(linkstate));
		// final Map<BGPTableType, Boolean> tableTypes = Maps.newHashMap();
		// tableTypes.put(ipv4, true);
		// tableTypes.put(linkstate,true);
		// tlvs.add(new GracefulCapability(true, 0, tableTypes));
		tlvs.add(new AS4BytesCapability(as));
		this.prefs = new BGPSessionPreferences(as, holdTimer, bgpId, tlvs);
	}

	@Override
	public BGPSessionPreferences getProposal() {
		return this.prefs;
	}

	/**
	 * @return the holdTimer
	 */
	public short getHoldTimer() {
		return this.holdTimer;
	}

	/**
	 * @return the as
	 */
	public ASNumber getAs() {
		return this.as;
	}

	/**
	 * @return the bgpId
	 */
	public IPv4Address getBgpId() {
		return this.bgpId;
	}
}
