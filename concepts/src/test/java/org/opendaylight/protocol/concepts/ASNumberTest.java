/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ASNumberTest {
	private ASNumber asn1, asn2, asn3, asn4;

	@Before
	public void setUp() {
		asn1 = new ASNumber(100, 200);
		asn2 = new ASNumber(6553800);
		asn3 = new ASNumber(0, 200);
		asn4 = new ASNumber(100, 199);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHighValueUnderflow() {
		new ASNumber(-1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHighValueOverflow() {
		new ASNumber(65536, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLowValueUnderflow() {
		new ASNumber(0, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLowValueOverflow() {
		new ASNumber(0, 65536);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAsnUnderflow() {
		new ASNumber(0, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAsnOverflow() {
		new ASNumber(4294967296L);
	}

	@Test
	public void testHashCode() {
		final Set<ASNumber> set = new HashSet<ASNumber>();

		set.add(asn1);
		assertEquals(1, set.size());

		set.add(asn2);
		assertEquals(1, set.size());

		set.add(asn3);
		assertEquals(2, set.size());
	}

	@Test
	public void testCompareTo() {
		final Set<ASNumber> set = new TreeSet<ASNumber>();

		set.add(asn1);
		assertEquals(1, set.size());

		set.add(asn2);
		assertEquals(1, set.size());

		set.add(asn3);
		assertEquals(2, set.size());

		set.add(asn4);
		assertEquals(3, set.size());
	}

	@Test
	public void testGetters() {
		assertEquals(100, asn1.getHighValue());
		assertEquals(200, asn1.getLowValue());
		assertEquals(6553800, asn1.getAsn());
	}

	@Test
	public void testEquals() {
		assertThat(asn1, equalTo(asn2));
		assertThat(asn1, not(equalTo(asn3)));
		assertThat(asn1, not(equalTo(asn4)));
		assertThat(asn1, not(equalTo(new Object())));
		assertFalse(asn1.equals(new Object()));
	}

	@Test
	public void testToString() {
		assertEquals("100.200", asn1.toString());
		assertEquals("200", asn3.toString());
	}
}

