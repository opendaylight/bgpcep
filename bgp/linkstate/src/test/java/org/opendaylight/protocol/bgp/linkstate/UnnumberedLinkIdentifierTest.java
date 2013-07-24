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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class UnnumberedLinkIdentifierTest {

	private UnnumberedLinkIdentifier unnumberedId;

	@Before
	public void init() {
		this.unnumberedId = new UnnumberedLinkIdentifier(256);
	}

	@Test
	public void testOverflows() {
		try {
			new UnnumberedLinkIdentifier(-2);
			fail("Constructor successful unexpectedly");
		} catch(final IllegalArgumentException e) {}
		try {
			new UnnumberedLinkIdentifier(4294967296L);
			fail("Constructor successful unexpectedly");
		} catch(final IllegalArgumentException e) {}
	}

	@Test
	public void testGetValue() {
		assertEquals(256L, this.unnumberedId.getValue());
	}

	/**
	 * Test hashcode(), equals(), compareTo()
	 */
	@Test
	public void test() {
		final UnnumberedLinkIdentifier testId1 = this.unnumberedId;
		final UnnumberedLinkIdentifier testId2 = new UnnumberedLinkIdentifier(256);
		final UnnumberedLinkIdentifier testId3 = new UnnumberedLinkIdentifier(951);

		assertEquals(testId1.hashCode(), testId2.hashCode());
		assertThat(testId2.hashCode(), not(equalTo(testId3.hashCode())));

		assertEquals(testId1, testId2);
		assertEquals(testId1.toString(), testId2.toString());
		assertThat(testId1, not(equalTo(testId3)));
		assertEquals(this.unnumberedId, testId1);
		assertNotNull(this.unnumberedId);
		assertThat(this.unnumberedId, not(equalTo(new Object())));

		assertEquals(0, testId1.compareTo(testId2));
		assertEquals(0, this.unnumberedId.compareTo(testId1));
		assertEquals(1, this.unnumberedId.compareTo(null));
		assertThat(testId2.compareTo(testId3), not(equalTo(0)));
		assertThat(testId3.compareTo(testId1), not(equalTo(0)));
	}

}
