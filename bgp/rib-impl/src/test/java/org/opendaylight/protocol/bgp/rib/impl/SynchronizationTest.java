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
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.parser.BGPLink;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;
import org.opendaylight.protocol.bgp.parser.impl.BGPUpdateMessageImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv4RouteImpl;
import org.opendaylight.protocol.bgp.util.BGPIPv6RouteImpl;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

import com.google.common.collect.Sets;

public class SynchronizationTest {

	private BGPSynchronization bs;

	private SimpleSessionListener listener;

	private BGPUpdateMessage ipv4m;

	private BGPUpdateMessage ipv6m;

	private BGPUpdateMessage lsm;

	@Before
	public void setUp() {
		this.listener = new SimpleSessionListener();
		final BGPIPv4RouteImpl i4 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("1.1.1.1/32"), new BaseBGPObjectState(null, null), null);
		this.ipv4m = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(i4), Collections.EMPTY_SET);
		final BGPIPv6RouteImpl i6 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("::1/32"), new BaseBGPObjectState(null, null), null);
		this.ipv6m = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(i6), Collections.EMPTY_SET);
		this.lsm = new BGPUpdateMessageImpl(Sets.<BGPObject> newHashSet(mock(BGPLink.class)), Collections.EMPTY_SET);

		final Set<BGPTableType> types = Sets.newHashSet(new BGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
				new BGPTableType(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

		this.bs = new BGPSynchronization(new BGPSession() {

			@Override
			public void close() {
			}

			@Override
			public Set<BGPTableType> getAdvertisedTableTypes() {
				return types;
			}
		}, this.listener, types);
	}

	@Test
	public void testSynchronize() {
		// simulate sync
		this.bs.updReceived(this.ipv6m);

		this.bs.updReceived(this.ipv4m);
		this.bs.updReceived(this.lsm);
		this.bs.kaReceived(); // nothing yet
		this.bs.updReceived(this.ipv4m);
		this.bs.kaReceived(); // linkstate
		assertEquals(1, this.listener.getListMsg().size());
		this.bs.kaReceived(); // ipv4 sync
		assertEquals(2, this.listener.getListMsg().size());
		assertEquals(Ipv4AddressFamily.class,
				((BGPUpdateSynchronizedImpl) this.listener.getListMsg().get(1)).getTableType().getAddressFamily());
	}
}
