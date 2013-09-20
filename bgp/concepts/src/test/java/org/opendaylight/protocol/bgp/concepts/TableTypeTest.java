/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpSubsequentAddressFamily;

public class TableTypeTest {

	@Test
	public void testTableTypes() {
		final BGPTableType tt1 = new BGPTableType(BgpAddressFamily.Ipv4, BgpSubsequentAddressFamily.MplsLabeledVpn);
		final BGPTableType tt2 = new BGPTableType(BgpAddressFamily.Ipv6, BgpSubsequentAddressFamily.valueOf("MplsLabeledVpn"));
		final BGPTableType tt3 = new BGPTableType(BgpAddressFamily.Ipv6, BgpSubsequentAddressFamily.Unicast);

		try {
			new BGPTableType(null, BgpSubsequentAddressFamily.MplsLabeledVpn);
			fail("Null AFI!");
		} catch (final NullPointerException e) {
			assertEquals("Address family may not be null", e.getMessage());
		}

		try {
			new BGPTableType(BgpAddressFamily.valueOf("Ipv6"), null);
			fail("Null SAFI!");
		} catch (final NullPointerException e) {
			assertEquals("Subsequent address family may not be null", e.getMessage());
		}

		assertFalse(tt1.equals(tt2));
		assertNotSame(tt1.hashCode(), tt2.hashCode());
		assertEquals(1, tt2.compareTo(tt1));
		assertEquals(1, tt2.compareTo(tt3));
		assertEquals(tt1.toString(), tt1.toString());
		assertNotSame(tt1.getAddressFamily(), tt2.getAddressFamily());
		assertEquals(tt1.getSubsequentAddressFamily(), tt2.getSubsequentAddressFamily());
	}

	@Test
	public void testOrigin() {
		final BgpOrigin or = BgpOrigin.Egp;
		assertEquals(or.name(), "Egp");
	}

	@Test
	public void testBaseBGPObjectState() {
		final BaseBGPObjectState state = new BaseBGPObjectState(BgpOrigin.Incomplete, null);
		final BaseBGPObjectState state1 = new BaseBGPObjectState(BgpOrigin.Incomplete, null);
		assertNull(state.getAggregator());
		assertEquals(BgpOrigin.Incomplete, state.getOrigin());
		assertEquals(state.toString(), state1.toString());

		final BaseBGPObjectState s = new BaseBGPObjectState(state);
		assertEquals(state, s);
		assertEquals(state.hashCode(), s.hashCode());

		assertEquals(s, s.newInstance());
	}
}
