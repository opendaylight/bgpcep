/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

import com.google.common.collect.Sets;

public class SynchronizationTest {

	private BGPSynchronization bs;

	private SimpleSessionListener listener;

	private Update ipv4m;

	private Update ipv6m;

	private Update lsm;

	@Before
	public void setUp() {
		this.listener = new SimpleSessionListener();
//		final BGPIPv4RouteImpl i4 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("1.1.1.1/32"), new BaseBGPObjectState(null, null), null);
//		this.ipv4m = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(i4), Collections.EMPTY_SET);
//		final BGPIPv6RouteImpl i6 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("::1/32"), new BaseBGPObjectState(null, null), null);
//		this.ipv6m = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(i6), Collections.EMPTY_SET);
//		this.lsm = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(mock(BGPLink.class)), Collections.EMPTY_SET);

		final Set<BgpTableType> types = Sets.newHashSet();
		types.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
		types.add(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

		this.bs = new BGPSynchronization(new BGPSession() {

			@Override
			public void close() {
			}

			@Override
			public Set<BgpTableType> getAdvertisedTableTypes() {
				return types;
			}
		}, this.listener, types);
	}

	@Test
	@Ignore
	//FIXME: to be fixed in testing phase
	public void testSynchronize() {
		// simulate sync
//		this.bs.updReceived(this.ipv6m);
//
//		this.bs.updReceived(this.ipv4m);
//		this.bs.updReceived(this.lsm);
//		this.bs.kaReceived(); // nothing yet
//		this.bs.updReceived(this.ipv4m);
		this.bs.kaReceived(); // linkstate
		assertEquals(1, this.listener.getListMsg().size());
		this.bs.kaReceived(); // ipv4 sync
		assertEquals(2, this.listener.getListMsg().size());
		assertEquals(Ipv4AddressFamily.class,
				((Update) this.listener.getListMsg().get(1)).getPathAttributes().getAugmentation(PathAttributes1.class).getMpReachNlri().getAfi());
	}
}
