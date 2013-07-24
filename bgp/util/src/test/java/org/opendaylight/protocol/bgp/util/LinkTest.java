/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import org.opendaylight.protocol.concepts.Metric;
import org.opendaylight.protocol.bgp.linkstate.LinkIdentifier;
import org.opendaylight.protocol.bgp.linkstate.LinkProtectionType;
import org.opendaylight.protocol.bgp.linkstate.TopologyIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NetworkLinkState;
import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;

import org.opendaylight.protocol.bgp.concepts.BGPOrigin;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.concepts.Community;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.util.BGPLinkImpl;
import org.opendaylight.protocol.util.DefaultingTypesafeContainer;

public class LinkTest {

	@Test
	public void testLinkImpl() {

		final BaseBGPObjectState state1 = new BaseBGPObjectState(BGPOrigin.EGP, null);
		final NetworkObjectState empty = new NetworkObjectState(null, Collections.<Community> emptySet(), Collections.<ExtendedCommunity> emptySet());
		final NetworkLinkState nstate1 = new NetworkLinkState(empty, new DefaultingTypesafeContainer<Metric<?>>(), null, LinkProtectionType.UNPROTECTED, null, null, null);

		final BGPLinkImpl link1 = new BGPLinkImpl(state1, new LinkIdentifier(new TopologyIdentifier(512), null, null), nstate1);
		final BGPLinkImpl link2 = new BGPLinkImpl(state1, new LinkIdentifier(new TopologyIdentifier(512), null, null), nstate1);

		assertEquals(link1, link2);
		assertEquals(link1.toString(), link2.toString());
		assertEquals(link1.hashCode(), link2.hashCode());
		assertEquals(link1.getLinkIdentifier(), link2.getLinkIdentifier());
		assertEquals(link1.currentState(), link2.currentState());
	}
}
