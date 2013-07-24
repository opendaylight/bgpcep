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

public class IPv6AddressTest {
	private IPv6Address a1, a2, a3, a4, a5;

	@Before
	public void setUp() throws Exception {
		this.a1 = new IPv6Address(
				InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7331"));
		this.a2 = new IPv6Address(
				InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7332"));
		this.a3 = new IPv6Address(
				InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7332"));
		this.a4 = new IPv6Address(
				InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:0000"));
		this.a5 = this.a4.applyMask(112);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailingFactory() {
		final byte[] fail_bytes = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
				14, 15, 16, 17 };

		new IPv6Address(fail_bytes);
	}

	@Test
	public void testFactory() {
		final byte[] succ_bytes = { 32, 1, 13, -72, -123, -93, 0, 0, 0, 0,
				-118, 46, 3, 112, 115, 49 };

		final IPv6Address a = new IPv6Address(succ_bytes);
		assertEquals(this.a1, a);
		assertEquals(succ_bytes, a.getAddress());
	}

	@Test
	public void testEquals() {
		assertTrue(this.a2.equals(this.a3));
		assertFalse(this.a1.equals(this.a2));
		assertFalse(this.a1.equals(new Object()));
	}

	@Test
	public void testHashCode() {
		final Set<IPv6Address> set = new HashSet<IPv6Address>();

		set.add(this.a1);
		assertEquals(1, set.size());

		set.add(this.a2);
		assertEquals(2, set.size());

		set.add(this.a3);
		assertEquals(2, set.size());

		set.add(this.a4);
		assertEquals(3, set.size());
	}

	@Test
	public void testCompareTo() {
		final Set<IPv6Address> set = new TreeSet<IPv6Address>();

		set.add(this.a1);
		assertEquals(1, set.size());

		set.add(this.a2);
		assertEquals(2, set.size());

		set.add(this.a3);
		assertEquals(2, set.size());

		set.add(this.a4);
		assertEquals(3, set.size());
	}

	@Test
	public void testCompareToExtended() throws Exception {
		IPv6Prefix an1 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("8:0:0:0:0:0:0:0")), 128);
		IPv6Prefix an2 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("1:0:0:0:0:0:0:0")), 128);

		assertEquals(7, an1.compareTo(an2));
		assertThat(an1, not(an2));

		assertEquals(-7, an2.compareTo(an1));
		assertThat(an2, not(an1));

		an1 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("aa:0:0:0:0:0:0:0")), 128);
		an2 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("1:0:0:0:0:0:0:0")), 128);

		assertEquals(169, an1.compareTo(an2));
		assertThat(an1, not(an2));

		assertEquals(-169, an2.compareTo(an1));
		assertThat(an2, not(an1));

		an1 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("ff:0:0:0:0:0:0:0")), 128);
		an2 = new IPv6Prefix(new IPv6Address(
				InetAddress.getByName("0:0:0:0:0:0:0:0")), 128);

		assertEquals(255, an1.compareTo(an2));
		assertThat(an1, not(an2));

		assertEquals(-255, an2.compareTo(an1));
		assertThat(an2, not(an1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument() throws Exception {
		new IPv6Address(InetAddress.getByName("120.20.20.20"));
	}

	@Test
	public void testToString() {
		assertEquals("2001:db8:85a3:0:0:8a2e:370:7331", this.a1.toString());
		assertEquals("2001:db8:85a3:0:0:8a2e:370:7332", this.a2.toString());
		assertEquals("2001:db8:85a3:0:0:8a2e:370:7332", this.a3.toString());
		assertEquals("2001:db8:85a3:0:0:8a2e:370:0", this.a4.toString());
		assertEquals("2001:db8:85a3:0:0:8a2e:370:0", this.a5.toString());
	}
}
