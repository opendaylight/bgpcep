/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.RouteOriginCommunity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class RouteOriginCommunityTest {

	private RouteOriginCommunity community;

	@Before
	public void init() {
		final AsNumber globalAdmin = new AsNumber(429496729800L);
		final byte[] localAdmin = new byte[] { 10, 0, 0, 1 };
		this.community = new RouteOriginCommunity(globalAdmin, localAdmin);
	}

	@Test
	public void testGetSubType() {
		assertEquals(3, this.community.getSubType());
	}

	@Test
	public void testGetGlobalAdmin() {
		final AsNumber testAsn = new AsNumber(429496729800L);
		assertEquals(this.community.getGlobalAdmin(), testAsn);
	}

	@Test
	public void testGetLocalAdmin() {
		assertArrayEquals(new byte[] { 10, 0, 0, 1 }, this.community.getLocalAdmin());
	}

	@Test
	public void testGetIanaAuthority() {
		assertFalse(this.community.getIanaAuthority());
	}

	@Test
	public void testIsTransitive() {
		assertFalse(this.community.isTransitive());
	}
}
