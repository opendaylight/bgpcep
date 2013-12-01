/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.List;

import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.CAs4BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.c.as4.bytes.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

import com.google.common.collect.Lists;

/**
 * Basic implementation of BGP Session Proposal. The values are taken from conf-bgp.
 */
public final class BGPSessionProposalImpl implements BGPSessionProposal {

	private final short holdTimer;

	private final int as;

	private final Ipv4Address bgpId;

	private final BGPSessionPreferences prefs;

	public BGPSessionProposalImpl(final short holdTimer, final int as, final Ipv4Address bgpId) {
		this.holdTimer = holdTimer;
		this.as = as;
		this.bgpId = bgpId;

		// FIXME: BUG-199: the reference to linkstate should be moved to config subsystem!
		final List<BgpParameters> tlvs = Lists.newArrayList();
		tlvs.add(new BgpParametersBuilder().setCParameters(
				new CMultiprotocolBuilder().setMultiprotocolCapability(
						new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build());
		tlvs.add(new BgpParametersBuilder().setCParameters(
				new CMultiprotocolBuilder().setMultiprotocolCapability(
						new MultiprotocolCapabilityBuilder().setAfi(LinkstateAddressFamily.class).setSafi(
								LinkstateSubsequentAddressFamily.class).build()).build()).build());
		tlvs.add(new BgpParametersBuilder().setCParameters(
				new CAs4BytesBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber((long) as)).build()).build()).build());
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
	public int getAs() {
		return this.as;
	}

	/**
	 * @return the bgpId
	 */
	public Ipv4Address getBgpId() {
		return this.bgpId;
	}
}
