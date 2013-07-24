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
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPOrigin;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import org.opendaylight.protocol.concepts.IPv6;

public class TableTypeTest {

	@Test
	public void testTableTypes() {
		final BGPTableType tt1 = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.MPLSLabeledVPN);
		final BGPTableType tt2 = new BGPTableType(BGPAddressFamily.IPv6, BGPSubsequentAddressFamily.valueOf("MPLSLabeledVPN"));
		final BGPTableType tt3 = new BGPTableType(BGPAddressFamily.IPv6, BGPSubsequentAddressFamily.Unicast);

		assertEquals(IPv6.FAMILY, BGPAddressFamily.IPv6.getAddressFamily());
		assertNull(BGPAddressFamily.LinkState.getAddressFamily());

		try {
			new BGPTableType(null, BGPSubsequentAddressFamily.MPLSLabeledVPN);
			fail("Null AFI!");
		} catch (final NullPointerException e) {
			assertEquals("Address family may not be null", e.getMessage());
		}

		try {
			new BGPTableType(BGPAddressFamily.valueOf("IPv6"), null);
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
		final BGPOrigin or = BGPOrigin.EGP;
		assertEquals(or.name(), "EGP");
	}

	@Test
	public void testBaseBGPObjectState() {
		final BaseBGPObjectState state = new BaseBGPObjectState(BGPOrigin.INCOMPLETE, null);
		final BaseBGPObjectState state1 = new BaseBGPObjectState(BGPOrigin.INCOMPLETE, null);
		assertNull(state.getAggregator());
		assertEquals(BGPOrigin.INCOMPLETE, state.getOrigin());
		assertEquals(state.toString(), state1.toString());

		final BaseBGPObjectState s = new BaseBGPObjectState(state);
		assertEquals(state, s);
		assertEquals(state.hashCode(), s.hashCode());

		assertEquals(s, s.newInstance());
	}
}
