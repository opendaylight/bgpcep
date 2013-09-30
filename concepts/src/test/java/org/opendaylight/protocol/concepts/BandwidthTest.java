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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;

public class BandwidthTest {
	private Bandwidth b1, b2, b3, b4;

	@Before
	public void setUp() {
		this.b1 = new Bandwidth(ByteArray.intToBytes(1000));
		this.b2 = new Bandwidth(ByteArray.intToBytes(2000));
		this.b3 = new Bandwidth(ByteArray.intToBytes(2000));
		this.b4 = new Bandwidth(ByteArray.intToBytes(100));
	}

	@Test
	public void testBitsBytes() {
		assertEquals(1000.0, ByteArray.bytesToInt(this.b1.getValue()), 0.1);
	}

	@Test
	public void testEquals() {
		assertFalse(this.b1.equals(null));
		assertThat(this.b1, not(equalTo(new Object())));
		assertThat(this.b1, equalTo(this.b1));
		assertThat(this.b1, not(equalTo(this.b2)));
		// FIXME BUG-98
		// assertEquals(this.b2, this.b3);
		assertFalse(this.b1.equals(new Object()));
	}

	@Test
	@Ignore
	// FIXME: BUG-89
	public void testToString() {
		final String s1 = "Bandwidth [_value=" + ByteArray.bytesToInt(this.b1.getValue()) + "]";
		assertEquals(s1, this.b1.toString());

	}

	@Test
	public void testHashCode() {
		final Set<Bandwidth> set = new HashSet<Bandwidth>();

		set.add(this.b1);
		assertEquals(1, set.size());

		set.add(this.b2);
		assertEquals(2, set.size());
		// FIXME BUG-98
		// set.add(this.b3);
		// assertEquals(2, set.size());

		set.add(this.b4);
		assertEquals(3, set.size());
	}
}
