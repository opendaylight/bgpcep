/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class IPv6PrefixTest {
	private Prefix<IPv6Address> p1, p2, p3, p4;
	private IPv6Address addr;

	@Before
	public void setUp() throws Exception {
		p1 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7331")), 128);
		p2 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7332")), 128);
		p3 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7332")), 128);
		p4 = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7300")), 120);
		addr = new IPv6Address(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7331"));
	}

	@Test
	public void testEquals() {
		assertTrue(p2.equals(p3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeLength() {
		new IPv6Prefix(addr, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLongLength() throws Exception {
		new IPv6Prefix(addr, 129);
	}

	@Test
	public void testHashCode() {
		final Set<Prefix<IPv6Address>> set = new HashSet<Prefix<IPv6Address>>();

		set.add(p1);
		assertEquals(1, set.size());

		set.add(p2);
		assertEquals(2, set.size());

		set.add(p3);
		assertEquals(2, set.size());

		set.add(p4);
		assertEquals(3, set.size());
	}

	@Test
	public void testCompareTo() {
		final Set<Prefix<IPv6Address>> set = new TreeSet<Prefix<IPv6Address>>();

		set.add(p1);
		assertEquals(1, set.size());

		set.add(p2);
		assertEquals(2, set.size());

		set.add(p3);
		assertEquals(2, set.size());

		set.add(p4);
		assertEquals(3, set.size());
	}
}

