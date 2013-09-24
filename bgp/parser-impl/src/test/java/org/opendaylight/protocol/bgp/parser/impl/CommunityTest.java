/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.OpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

public class CommunityTest {

	@Test
	public void testCommunity() {
		CommunityUtil.create(10, 222);
		final Community c = CommunityUtil.create(12, 12);
		assertEquals(12, c.getAsNumber().getValue().intValue());
		assertEquals(12, c.getSemantics().intValue());
	}

	@Test
	@Ignore
	// FIXME: range is not implemented
	public void testOverflows() {
		try {
			CommunityUtil.create(10, -2);
			fail("Semantics under range.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid semantics specified", e.getMessage());
		}
		try {
			CommunityUtil.create(10, 65536);
			fail("Semantics above range.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid semantics specified", e.getMessage());
		}
	}

	@Test
	public void testToString() {
		final Community c = CommunityUtil.create(10, 222);
		assertNotNull(c.toString());
	}

	@Test
	public void testValueOf() {
		final Community comm = CommunityUtil.valueOf("12:50");
		assertEquals(12, comm.getAsNumber().getValue().intValue());
		assertEquals(50, comm.getSemantics().intValue());
	}

	@Test
	public void testExtendedCommunity() {
		final ExtendedCommunity ec = new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 });
		final Object ec2 = new RouteOriginCommunity(new AsNumber((long) 84), new byte[] { 1, 2, 3, 4 });
		assertNotSame(ec, ec2);
		assertEquals(ec, new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 }));
		assertEquals(ec.hashCode(), (new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 })).hashCode());
	}
}
