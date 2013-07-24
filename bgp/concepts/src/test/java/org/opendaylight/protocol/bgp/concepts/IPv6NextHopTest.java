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
import org.opendaylight.protocol.bgp.concepts.IPv6NextHop;

import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;

public class IPv6NextHopTest {

	private IPv6NextHop nextHopA;
	private IPv6NextHop nextHopB;

	@Before
	public void init() {
		final IPv6Address global = IPv6.FAMILY.addressForString("2001:db8:85a3:0:0:8a2e:370:7331");
		final IPv6Address local = IPv6.FAMILY.addressForString("2001:db8:85a3:0:0:8a2e:370:0000");
		this.nextHopA = new IPv6NextHop(global);
		this.nextHopB = new IPv6NextHop(global, local);
	}

	@Test
	public void testGetGlobal() {
		final IPv6Address globalTestAddress = IPv6.FAMILY.addressForString("2001:db8:85a3:0:0:8a2e:370:7331");

		assertEquals(this.nextHopA.getGlobal(), globalTestAddress);
		assertEquals(this.nextHopB.getGlobal(), globalTestAddress);
	}

	@Test
	public void testGetLinkLocal() {
		final IPv6Address localTestAddress = IPv6.FAMILY.addressForString("2001:db8:85a3:0:0:8a2e:370:0000");

		assertNull(this.nextHopA.getLinkLocal());
		assertEquals(this.nextHopB.getLinkLocal(), localTestAddress);
	}

	@Test
	public void testHashCode() {
		final IPv6NextHop nextHop1 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331");
		final IPv6NextHop nextHop2 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331", "2001:db8:85a3:0:0:8a2e:370:0000");
		assertEquals(this.nextHopA.hashCode(), nextHop1.hashCode());
		assertEquals(this.nextHopB.hashCode(), nextHop2.hashCode());
	}

	@Test
	public void testEquals() {
		assertNotNull(this.nextHopA);
		assertNotNull(this.nextHopB);
		assertThat(this.nextHopA, not(new Object()));
		assertThat(this.nextHopB, not(new Object()));

		final IPv6NextHop nextHop1 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331");
		final IPv6NextHop nextHop2 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331", "2001:db8:85a3:0:0:8a2e:370:0000");
		assertEquals(this.nextHopA, nextHop1);
		assertEquals(this.nextHopB, nextHop2);

		final IPv6NextHop x = this.nextHopA;
		assertEquals(this.nextHopA, x);

		final IPv6NextHop y = this.nextHopB;
		assertEquals(this.nextHopB, y);

		final IPv6NextHop nextHop3 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7332");
		final IPv6NextHop nextHop4 = IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7332", "2001:db8:85a3:0:0:8a2e:370:0000");
		assertThat(this.nextHopA, not(nextHop3));
		assertThat(this.nextHopA, not(nextHop4));
		assertThat(this.nextHopB, not(nextHop3));
		assertThat(this.nextHopB, not(nextHop4));
	}

	@Test
	public void testToString() {
		assertNotNull(this.nextHopA.toString());
		assertNotNull(this.nextHopB.toString());
	}

	@Test
	public void testForString() {
		assertEquals(this.nextHopA, IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331"));
		assertEquals(this.nextHopB, IPv6NextHop.forString("2001:db8:85a3:0:0:8a2e:370:7331", "2001:db8:85a3:0:0:8a2e:370:0000"));
	}
}
