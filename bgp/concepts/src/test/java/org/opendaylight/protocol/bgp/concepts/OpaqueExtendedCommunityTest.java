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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.opaque.extended.community.OpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.opaque.extended.community.OpaqueExtendedCommunityBuilder;

public class OpaqueExtendedCommunityTest {

	private boolean transitive;
	private byte[] value;

	private OpaqueExtendedCommunity community;

	@Before
	public void init() {
		this.transitive = true;
		this.value = new byte[] { 1, 5, 9, 3, 5, 7 };
		this.community = new OpaqueExtendedCommunityBuilder().setTransitive(this.transitive).setValue(this.value).build();
	}

	@Test
	@Ignore
	// FIXME: when length is implemented
	public void testOverflows() {
		try {
			new OpaqueExtendedCommunityBuilder().setTransitive(this.transitive).setValue(new byte[] { 0, 1, 2, 3, 4, 5, 6, }).build();
			fail("Constructor successful unexpectedly");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid value", e.getMessage());
		}
	}

	@Test
	public void testGetValue() {
		assertArrayEquals(new byte[] { 1, 5, 9, 3, 5, 7 }, this.community.getValue());
	}

	@Test
	public void testIsTransitive() {
		assertTrue(this.community.isTransitive());
	}

	@Test
	public void testToString() {
		final OpaqueExtendedCommunity c = new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(this.value).build();
		assertNotSame(c.toString(), this.community.toString());
	}
}
