/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class IPv4PrefixTest {
	private Prefix<IPv4Address> p1, p2, p3, p4;
	private IPv4Address addr;

	@Before
	public void setUp() throws Exception {
		p1 = new IPv4Prefix(new IPv4Address(InetAddress.getByName("10.0.0.1")), 32);
		p2 = new IPv4Prefix(new IPv4Address(InetAddress.getByName("10.0.0.2")), 32);
		p3 = new IPv4Prefix(new IPv4Address(InetAddress.getByName("10.0.0.2")), 32);
		p4 = new IPv4Prefix(new IPv4Address(InetAddress.getByName("10.0.0.0")), 24);
		addr = new IPv4Address(InetAddress.getByName("1.2.3.4"));
	}

	@Test
	public void testGetLength() {
		assertEquals(32, p1.getLength());
		assertEquals(32, p2.getLength());
		assertEquals(32, p3.getLength());
		assertEquals(24, p4.getLength());
	}

	@Test
	public void testEquals() {
		Prefix<IPv4Address> p5 = p4;
		assertEquals(p4, p5);
		assertNotNull(p4);
		assertThat(p4, not(new Object()));
		assertEquals(p2, p3);
	}

	@Test
	public void testHashCode() {
		final Set<Prefix<IPv4Address>> set = new HashSet<Prefix<IPv4Address>>();

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
		final Set<Prefix<IPv4Address>> set = new TreeSet<Prefix<IPv4Address>>();

		set.add(p1);
		assertEquals(1, set.size());

		set.add(p2);
		assertEquals(2, set.size());

		set.add(p3);
		assertEquals(2, set.size());

		set.add(p4);
		assertEquals(3, set.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeLength() {
		new IPv4Prefix(addr, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLongLength() {
		new IPv4Prefix(addr, 33);
	}

	@Test
	public void testToString() {
		String name1 = p1.getAddress().toString() +"/"+ p1.getLength();
		assertEquals(name1, p1.toString());

		String name2 = p2.getAddress().toString() +"/"+ p2.getLength();
		assertEquals(name2, p2.toString());
	}

	@Test
	public void testMatches() throws Exception {
		IPv4Address address1 = new IPv4Address(InetAddress.getByName("10.0.0.1"));
		IPv4Address address2 = new IPv4Address(InetAddress.getByName("10.0.0.2"));
		IPv4Prefix p1 = new IPv4Prefix(address1, 32);
		IPv4Prefix p2 = new IPv4Prefix(address1, 16);
		assertTrue(p1.contains(address1));
		assertFalse(p1.contains(address2));
		assertTrue(p2.contains(address1));
		assertTrue(p2.contains(address2));
	}

	@Test
	public void testApplyMask() throws Exception {
		final IPv4Address addr = new IPv4Address(InetAddress.getByName("10.1.2.3"));
		final IPv4Prefix p5 = new IPv4Prefix(addr, 30);
		final IPv4Prefix p6 = new IPv4Prefix(addr, 31);
		final IPv4Prefix p7 = new IPv4Prefix(addr, 32);
		final IPv4Prefix p8 = new IPv4Prefix(addr, 16);

		assertFalse(addr == p5.getAddress());
		assertFalse(addr == p6.getAddress());
		assertFalse(addr == p7.getAddress());
		assertEquals("10.1.2.3", addr.toString());
		assertEquals("10.1.2.0/30", p5.toString());
		assertEquals("10.1.2.2/31", p6.toString());
		assertEquals("10.1.2.3/32", p7.toString());
		assertEquals("10.1.0.0/16", p8.toString());
	}
}

