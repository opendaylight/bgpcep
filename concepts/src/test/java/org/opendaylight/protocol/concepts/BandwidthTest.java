/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class BandwidthTest {
	private Bandwidth b1, b2, b3, b4;

	@Before
	public void setUp() {
		b1 = new Bandwidth(1000);
		b2 = new Bandwidth(2000);
		b3 = new Bandwidth(2000);
		b4 = new Bandwidth(100);
	}

	@Test
	public void testBitsBytes() {
		assertEquals(8000, b1.getBitsPerSecond());
		assertEquals(1000.0, b1.getBytesPerSecond(), 0.1);
	}

	@Test
	public void testEquals() {
		assertFalse(b1.equals(null));
		assertThat(b1, not(equalTo(new Object())));
		assertThat(b1, equalTo(b1));
		assertThat(b1, not(equalTo(b2)));
		assertThat(b2, equalTo(b3));
		assertFalse(b1.equals(new Object()));
	}

	@Test
	public void testToString(){
		String s1 = "Bandwidth [bytesPerSecond=" + b1.getBytesPerSecond() + "]";
		assertEquals(s1, b1.toString());

	}

	@Test
	public void testHashCode() {
		final Set<Bandwidth> set = new HashSet<Bandwidth>();

		set.add(b1);
		assertEquals(1, set.size());

		set.add(b2);
		assertEquals(2, set.size());

		set.add(b3);
		assertEquals(2, set.size());

		set.add(b4);
		assertEquals(3, set.size());
	}

	@Test
	public void testCompareTo() {
		final Set<Bandwidth> set = new TreeSet<Bandwidth>();

		set.add(b1);
		assertEquals(1, set.size());

		set.add(b2);
		assertEquals(2, set.size());

		set.add(b3);
		assertEquals(2, set.size());

		set.add(b4);
		assertEquals(3, set.size());
	}
}

