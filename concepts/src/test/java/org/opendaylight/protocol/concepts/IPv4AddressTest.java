/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class IPv4AddressTest {
	private IPv4Address a1, a2, a3, a4, a5;

	@Before
	public void setUp() {
		this.a1 = IPv4.FAMILY.addressForString("10.0.0.1");
		this.a2 = IPv4.FAMILY.addressForString("10.0.0.2");
		this.a3 = IPv4.FAMILY.addressForString("10.0.0.2");
		this.a4 = IPv4.FAMILY.addressForString("10.0.0.0");
		this.a5 = this.a4.applyMask(24);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFactoryFailure() {
		final byte[] fail_bytes = { 1, 2, 3, 4, 5 };
		new IPv4Address(fail_bytes);
	}

	@Test
	public void testFactorySuccess() {
		final byte[] succ_bytes = { 10, 0, 0, 1 };

		final IPv4Address a = new IPv4Address(succ_bytes);
		assertEquals(this.a1, a);
		assertArrayEquals(a.getAddress(), succ_bytes);
	}

	@Test
	public void testEquals() {
		assertEquals(this.a2, this.a3);
		assertFalse(this.a1.equals(this.a2));
		assertFalse(this.a1.equals(new Object()));
	}

	@Test
	public void testHashCode() {
		final Set<IPv4Address> set = Sets.newHashSet();

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
	public void testCompareTo() throws Exception {
		final Set<IPv4Address> set = Sets.newTreeSet();

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
	public void testCompareToExtended() {
		IPv4Address an1 = IPv4.FAMILY.addressForString("192.168.4.5");
		IPv4Address an2 = IPv4.FAMILY.addressForString("190.168.4.5");

		assertEquals(2, an1.compareTo(an2));
		assertFalse(an1.equals(an2));

		assertEquals(-2, an2.compareTo(an1));
		assertFalse(an2.equals(an1));

		an1 = IPv4.FAMILY.addressForString("192.10.4.5");
		an2 = IPv4.FAMILY.addressForString("10.10.4.5");

		assertEquals(182, an1.compareTo(an2));
		assertFalse(an1.equals(an2));

		assertEquals(-182, an2.compareTo(an1));
		assertFalse(an2.equals(an1));

		an1 = IPv4.FAMILY.addressForString("255.10.4.5");
		an2 = IPv4.FAMILY.addressForString("0.10.4.5");

		assertEquals(255, an1.compareTo(an2));
		assertFalse(an1.equals(an2));

		assertEquals(-255, an2.compareTo(an1));
		assertFalse(an2.equals(an1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument() {
		IPv4.FAMILY.addressForString("2001:db8:85a3:0:0:8a2e:370:7332");
	}

	@Test
	public void testToString() {
		assertEquals("10.0.0.1", this.a1.toString());
		assertEquals("10.0.0.2", this.a2.toString());
		assertEquals("10.0.0.2", this.a3.toString());
		assertEquals("10.0.0.0", this.a4.toString());
		assertEquals("10.0.0.0", this.a5.toString());
	}
}
