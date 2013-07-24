/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.Inet4SpecificExtendedCommunity;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;

public class Inet4SpecificExtendedCommunityTest {

	private boolean transitive;
	private int subType;
	private IPv4Address globalAdmin;
	private byte[] localAdmin;

	private Inet4SpecificExtendedCommunity community;

	@Before
	public void init() {
		this.transitive = true;
		this.subType = 123;
		this.globalAdmin = IPv4.FAMILY.addressForString("10.0.0.1");
		this.localAdmin = new byte[] { 10, 1 };
		this.community = new Inet4SpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, this.localAdmin);
	}

	@Test
	public void testOverflows() {
		try {
			new Inet4SpecificExtendedCommunity(this.transitive, -2, this.globalAdmin, this.localAdmin);
			fail("Sub-type is negative!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Sub-Type -2", e.getMessage());
		}
		try {
			new Inet4SpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, new byte[] { 10, 0, 1 });
			fail("Invalid length of local administrator!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Local Administrator", e.getMessage());
		}
		try {
			new Inet4SpecificExtendedCommunity(this.transitive, 256, this.globalAdmin, this.localAdmin);
			fail("Sub-type is above range!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Sub-Type 256", e.getMessage());
		}
	}

	@Test
	public void testGetSubType() {
		final int subType = 123;
		assertEquals(subType, this.community.getSubType());
	}

	@Test
	public void testGetGlobalAdmin() {
		final IPv4Address globalAdmin = IPv4.FAMILY.addressForString("10.0.0.1");
		assertEquals(globalAdmin, this.community.getGlobalAdmin());
	}

	@Test
	public void testGetLocalAdmin() {
		final byte[] localAdmin = new byte[] { 10, 1 };
		assertArrayEquals(localAdmin, this.community.getLocalAdmin());
	}

	@Test
	public void testGetIanaAuthority() {
		// Should be always false for Inet4SpecificExtendedCommunity instances
		assertFalse(this.community.getIanaAuthority());
	}

	@Test
	public void testIsTransitive() {
		assertTrue(this.community.isTransitive());
	}

	@Test
	public void testToString() {
		final Inet4SpecificExtendedCommunity comm = new Inet4SpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, this.localAdmin);
		assertEquals(this.community.toString(), comm.toString());
		assertEquals(this.community, comm);
		assertEquals(this.community.hashCode(), comm.hashCode());
	}
}
