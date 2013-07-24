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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.IPv4NextHop;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;

public class IPv4NextHopTest {

	private IPv4NextHop nextHop;

	@Before
	public void init() {
		final IPv4Address address = IPv4.FAMILY.addressForString("10.0.0.1");
		this.nextHop = new IPv4NextHop(address);
	}

	@Test
	public void testGetGlobal() {
		final IPv4Address address = IPv4.FAMILY.addressForString("10.0.0.1");
		assertEquals(address, this.nextHop.getGlobal());
	}

	@Test
	public void testGetLinkLocal() {
		assertNull(this.nextHop.getLinkLocal());
	}

	@Test
	public void testHashCode() {
		final IPv4NextHop nextHop2 = IPv4NextHop.forString("10.0.0.1");
		assertEquals(this.nextHop.hashCode(), nextHop2.hashCode());
	}

	@Test
	public void testEquals() {
		assertNotNull(this.nextHop);
		assertThat(this.nextHop, not(new Object()));

		final IPv4NextHop nextHop1 = this.nextHop;
		assertEquals(this.nextHop, nextHop1);

		final IPv4NextHop nextHop2 = IPv4NextHop.forString("10.0.0.1");
		assertEquals(this.nextHop, nextHop2);
	}

	@Test
	public void testToString() {
		assertNotNull(this.nextHop.toString());
	}

	@Test
	public void testForString() {
		assertEquals(this.nextHop, IPv4NextHop.forString("10.0.0.1"));
	}
}
