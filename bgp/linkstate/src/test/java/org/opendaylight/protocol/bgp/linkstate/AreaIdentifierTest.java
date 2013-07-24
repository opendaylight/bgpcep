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

import org.junit.Before;
import org.junit.Test;

public class AreaIdentifierTest {

	private AreaIdentifier identifier;

	@Before
	public void init() {
		final byte[] id = new byte[] {10,0,0,1};
		this.identifier = new AreaIdentifier(id);
	}


	@Test(expected=IllegalArgumentException.class)
	public void testLinkAreaIdentifier() {
		final byte[] id = new byte[] {10, 90, 13, 9, 3};
		new AreaIdentifier(id);
	}

	@Test
	public void testGetBytes() {
		final byte[] id = new byte[] {10,0,0,1};
		assertArrayEquals(id, this.identifier.getBytes());
	}

	@Test
	public void testHashCode() {
		final byte[] id1 = new byte[] {10,0,0,1};
		final AreaIdentifier testIdentifier1 = new AreaIdentifier(id1);
		assertEquals(this.identifier.hashCode(), testIdentifier1.hashCode());

		final byte[] id2 = new byte[] {10,0,0,2};
		final AreaIdentifier testIdentifier2 = new AreaIdentifier(id2);
		assertThat(this.identifier.compareTo(testIdentifier2), not(equalTo(0)));
	}

	@Test
	public void testEquals() {
		assertThat(this.identifier, not(new Object()));

		final byte[] id = new byte[] {10,0,0,1};
		final AreaIdentifier testIdentifier = new AreaIdentifier(id);
		assertEquals(this.identifier, testIdentifier);
	}

	@Test
	public void testCompareTo() {
		final byte[] id1 = new byte[] {10,0,0,1};
		final AreaIdentifier testIdentifier1 = new AreaIdentifier(id1);
		assertEquals(0, this.identifier.compareTo(testIdentifier1));

		final byte[] id2 = new byte[] {10,0,0,2};
		final AreaIdentifier testIdentifier2 = new AreaIdentifier(id2);
		assertThat(this.identifier.compareTo(testIdentifier2), not(equalTo(0)));
	}

	@Test
	public void testToString(){
		final byte[] id1 = new byte[] {10,0,0,1};
		final AreaIdentifier l1 = new AreaIdentifier(id1);
		final String s1 = "AreaIdentifier{id=0a.00.00.01}";
		assertEquals(s1, l1.toString());
	}

}
