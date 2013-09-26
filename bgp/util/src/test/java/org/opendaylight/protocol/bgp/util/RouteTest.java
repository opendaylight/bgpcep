/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.CIpv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv4.next.hop.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.c.ipv6.next.hop.Ipv6NextHopBuilder;

public class RouteTest {

	BaseBGPObjectState base = new BaseBGPObjectState(BgpOrigin.Egp, null);
	final NetworkRouteState prefix4State = new NetworkRouteState(new CIpv4NextHopBuilder().setIpv4NextHop(
			new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("128.54.8.9")).build()).build());
	final NetworkRouteState prefix6State = new NetworkRouteState(new CIpv6NextHopBuilder().setIpv6NextHop(
			new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001::4")).build()).build());
	final BGPIPv4RouteImpl r4 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("172.168.4.6/24"), this.base, this.prefix4State);
	final BGPIPv6RouteImpl r6 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("2001::4/32"), this.base, this.prefix6State);

	@Test
	public void testIPv4Route() {
		final BGPIPv4RouteImpl r2 = new BGPIPv4RouteImpl(IPv4.FAMILY.prefixForString("172.168.4.6/24"), this.base, this.prefix4State);

		assertEquals(this.r4, r2);
		assertEquals(this.r4.hashCode(), r2.hashCode());
		assertNotSame(this.r4, this.r6);
	}

	@Test
	public void testIPv6Route() {
		final BGPIPv6RouteImpl r2 = new BGPIPv6RouteImpl(IPv6.FAMILY.prefixForString("2001::4/32"), this.base, this.prefix6State);

		assertEquals(this.r6.currentState(), r2.currentState());
		assertEquals(this.r6.toString(), r2.toString());
		assertEquals(this.r6.getName(), r2.getName());
	}
}
