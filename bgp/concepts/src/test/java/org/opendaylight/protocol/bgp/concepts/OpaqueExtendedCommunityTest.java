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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.OpaqueExtendedCommunity;

public class OpaqueExtendedCommunityTest {

	private boolean transitive;
	private int subType;
	private byte[] value;

	private OpaqueExtendedCommunity community;

	@Before
	public void init() {
		this.transitive = true;
		this.subType = 222;
		this.value = new byte[] { 1, 5, 9, 3, 5, 7 };
		this.community = new OpaqueExtendedCommunity(this.transitive, this.subType, this.value);
	}

	@Test
	public void testOverflows() {
		try {
			new OpaqueExtendedCommunity(this.transitive, -2, this.value);
			fail("Sub-type is negative!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Sub-Type", e.getMessage());
		}
		try {
			new OpaqueExtendedCommunity(this.transitive, 256, this.value);
			fail("Sub-type is above range!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Sub-Type", e.getMessage());
		}
		try {
			new OpaqueExtendedCommunity(this.transitive, this.subType, new byte[] { 0, 1, 2, 3, 4, 5, 6, });
			fail("Constructor successful unexpectedly");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid value", e.getMessage());
		}
	}

	@Test
	public void testGetSubType() {
		assertEquals(222, this.community.getSubType());
	}

	@Test
	public void testGetValue() {
		assertArrayEquals(new byte[] { 1, 5, 9, 3, 5, 7 }, this.community.getValue());
	}

	@Test
	public void testGetIanaAuthority() {
		assertFalse(this.community.getIanaAuthority());
	}

	@Test
	public void testIsTransitive() {
		assertTrue(this.community.isTransitive());
	}

	@Test
	public void testToString() {
		final OpaqueExtendedCommunity c = new OpaqueExtendedCommunity(false, this.subType, this.value);
		assertNotSame(c.toString(), this.community.toString());
	}
}
