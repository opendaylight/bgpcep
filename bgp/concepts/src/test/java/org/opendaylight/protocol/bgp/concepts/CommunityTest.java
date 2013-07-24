/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.Community;
import org.opendaylight.protocol.bgp.concepts.ExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.OpaqueExtendedCommunity;
import org.opendaylight.protocol.bgp.concepts.RouteOriginCommunity;

import org.opendaylight.protocol.concepts.ASNumber;

public class CommunityTest {

	@Test
	public void testCommunity() {
		new Community(new ASNumber(0, 10), 222);
		final ASNumber as = new ASNumber(12);
		final Community c = new Community(as, 12);
		assertEquals(as, c.getAs());
		assertEquals(12, c.getSemantics());
	}

	@Test
	public void testOverflows() {
		try {
			new Community(new ASNumber(0, 10), -2);
			fail("Semantics under range.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid semantics specified", e.getMessage());
		}
		try {
			new Community(new ASNumber(5, 10), 5);
			fail("AS high number cannot be null");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid AS number specified", e.getMessage());
		}
		try {
			new Community(new ASNumber(0, 10), 65536);
			fail("Semantics above range.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid semantics specified", e.getMessage());
		}
	}

	@Test
	public void testEquals() {
		final Community c1 = new Community(new ASNumber(0, 10), 222);
		final Community c2 = new Community(new ASNumber(0, 10), 222);
		assertEquals(c1, c2);
		assertThat(c1, not(new Object()));
	}

	@Test
	public void testHashCode() {
		final Community c1 = new Community(new ASNumber(0, 10), 222);
		final Community c2 = new Community(new ASNumber(0, 10), 222);
		assertEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	public void testToString() {
		final Community c = new Community(new ASNumber(0, 10), 222);
		assertNotNull(c.toString());
	}

	@Test
	public void testValueOf() {
		final Community comm = Community.valueOf("12:50");
		assertEquals(comm, new Community(new ASNumber(0, 12), 50));
	}

	@Test
	public void testExtendedCommunity() {
		final ExtendedCommunity ec = new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 });
		final Object ec2 = new RouteOriginCommunity(new ASNumber(84), new byte[] { 1, 2, 3, 4 });
		assertNotSame(ec, ec2);
		assertEquals(ec, new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 }));
		assertEquals(ec.hashCode(), (new OpaqueExtendedCommunity(false, 5, new byte[] { 1, 2, 3, 4, 5, 6 })).hashCode());
	}
}
