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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.inet4.specific.extended.community.Inet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.inet4.specific.extended.community.Inet4SpecificExtendedCommunityBuilder;

public class Inet4SpecificExtendedCommunityTest {

	private boolean transitive;
	private Ipv4Address globalAdmin;
	private byte[] localAdmin;

	private Inet4SpecificExtendedCommunity community;

	@Before
	public void init() {
		this.transitive = true;
		this.globalAdmin = new Ipv4Address("10.0.0.1");
		this.localAdmin = new byte[] { 10, 1 };
		this.community = new Inet4SpecificExtendedCommunityBuilder().setTransitive(this.transitive).setGlobalAdministrator(this.globalAdmin).setLocalAdministrator(
				this.localAdmin).build();
	}

	@Test
	@Ignore
	// FIXME: length is not implemented
	public void testOverflows() {
		try {
			new Inet4SpecificExtendedCommunityBuilder().setTransitive(this.transitive).setGlobalAdministrator(this.globalAdmin).setLocalAdministrator(
					new byte[] { 10, 0, 1 }).build();
			fail("Invalid length of local administrator!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Local Administrator", e.getMessage());
		}
	}

	@Test
	public void testGetGlobalAdmin() {
		final Ipv4Address globalAdmin = new Ipv4Address("10.0.0.1");
		assertEquals(globalAdmin, this.community.getGlobalAdministrator());
	}

	@Test
	public void testGetLocalAdmin() {
		final byte[] localAdmin = new byte[] { 10, 1 };
		assertArrayEquals(localAdmin, this.community.getLocalAdministrator());
	}

	@Test
	public void testIsTransitive() {
		assertTrue(this.community.isTransitive());
	}
}
