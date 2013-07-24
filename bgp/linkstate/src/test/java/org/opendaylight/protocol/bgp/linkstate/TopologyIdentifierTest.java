/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TopologyIdentifierTest {

	private TopologyIdentifier identifier;

	@Before
	public void init() {
		this.identifier = new TopologyIdentifier(23);
	}

	@Test
	public void testLinkAreaIdentifier() {
		try {
			new TopologyIdentifier(4096);
		} catch (final Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
		try {
			new TopologyIdentifier(-2);
		} catch (final Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

	}

	@Test
	public void testGetId() {
		assertEquals(23, this.identifier.getId());
	}

	@Test
	public void testHashCode() {
		final TopologyIdentifier testIdentifier1 = new TopologyIdentifier(23);
		assertEquals(this.identifier.hashCode(), testIdentifier1.hashCode());

		final TopologyIdentifier testIdentifier2 = new TopologyIdentifier(24);
		assertThat(this.identifier.compareTo(testIdentifier2), not(equalTo(0)));
	}

	@Test
	public void testEquals() {
		assertThat(this.identifier, not(new Object()));

		final TopologyIdentifier testIdentifier = new TopologyIdentifier(23);
		assertEquals(this.identifier, testIdentifier);
	}

	@Test
	public void testCompareTo() {
		final TopologyIdentifier testIdentifier1 = new TopologyIdentifier(23);
		assertEquals(0, this.identifier.compareTo(testIdentifier1));

		final TopologyIdentifier testIdentifier2 = new TopologyIdentifier(34);
		assertThat(this.identifier.compareTo(testIdentifier2), not(equalTo(0)));
	}

	@Test
	public void testToString() {
		assertEquals("TopologyIdentifier{id=23}", this.identifier.toString());
		final TopologyIdentifier id = new TopologyIdentifier(3);
		assertEquals("TopologyIdentifier{id=3}", id.toString());
	}
}
