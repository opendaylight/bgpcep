/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class ISISAreaIdentifierTest {

	private ISISAreaIdentifier identifier;

	@Before
	public void init() {
		final byte[] id = new byte[] { 10, 0, 0, 1 };
		this.identifier = new ISISAreaIdentifier(id);
	}

	@Test
	public void testOverflows() {
		try {
			new ISISAreaIdentifier(new byte[] {});
			fail("Constructor successful unexpectedly");
		} catch (final IllegalArgumentException e) {
		}
		try {
			new ISISAreaIdentifier(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2 });
			fail("Constructor successful unexpectedly");
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testGetBytes() {
		assertArrayEquals(new byte[] { 10, 0, 0, 1 }, this.identifier.getBytes());
	}

	@Test
	public void testHashCode() {
		final byte[] id1 = new byte[] { 10, 0, 0, 1 };
		final ISISAreaIdentifier testIdentifier1 = new ISISAreaIdentifier(id1);
		assertEquals(testIdentifier1.hashCode(), this.identifier.hashCode());

		final byte[] id2 = new byte[] { 10, 0, 0, 2 };
		final ISISAreaIdentifier testIdentifier2 = new ISISAreaIdentifier(id2);
		assertThat(testIdentifier2.hashCode(), not(equalTo(this.identifier.hashCode())));
	}

	@Test
	public void testEqualsObject() {
		assertThat(this.identifier, not(new Object()));

		final byte[] id = new byte[] { 10, 0, 0, 1 };
		final ISISAreaIdentifier testIdentifier = new ISISAreaIdentifier(id);
		assertEquals(this.identifier, testIdentifier);
	}

	@Test
	public void testCompareTo() {
		final byte[] id1 = new byte[] { 10, 0, 0, 1 };
		final ISISAreaIdentifier testIdentifier1 = new ISISAreaIdentifier(id1);
		assertEquals(0, this.identifier.compareTo(testIdentifier1));

		final byte[] id2 = new byte[] { 10, 0, 0, 2 };
		final ISISAreaIdentifier testIdentifier2 = new ISISAreaIdentifier(id2);
		assertThat(this.identifier.compareTo(testIdentifier2), not(equalTo(0)));
	}

	@Test(expected = NullPointerException.class)
	public void testIllegalArgument() {
		new ISISAreaIdentifier(null);
	}

	@Test
	public void testToString() {
		assertEquals("ISISAreaIdentifier{id=0a.00.00.01}", this.identifier.toString());
		final ISISAreaIdentifier id = new ISISAreaIdentifier(new byte[] { 1 });
		assertEquals("ISISAreaIdentifier{id=01}", id.toString());
	}
}
