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

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;

public class RouteOriginCommunityTest {

	private RouteOriginExtendedCommunity community;

	@Before
	public void init() {
		final AsNumber globalAdmin = new AsNumber(65535L);
		final byte[] localAdmin = new byte[] { 10, 0, 0, 1 };
		this.community = new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(globalAdmin).setLocalAdministrator(localAdmin).build();
	}

	@Test
	public void testGetGlobalAdmin() {
		final AsNumber testAsn = new AsNumber(65535L);
		assertEquals(this.community.getGlobalAdministrator(), testAsn);
	}

	@Test
	public void testGetLocalAdmin() {
		assertArrayEquals(new byte[] { 10, 0, 0, 1 }, this.community.getLocalAdministrator());
	}
}
