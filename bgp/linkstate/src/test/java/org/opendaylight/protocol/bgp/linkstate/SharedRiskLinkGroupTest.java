/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;

public class SharedRiskLinkGroupTest {

	@Test
	public void testOverflows() {
		try {
			new SharedRiskLinkGroup(-2);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
		try {
			new SharedRiskLinkGroup(4294967296L);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
	}

	@Test
	public void testGetValue() {
		SharedRiskLinkGroup group = new SharedRiskLinkGroup(252);
		assertEquals(252, group.getValue());
	}

	@Test
	public void testHashCodeEquals() {
		SharedRiskLinkGroup group1 = new SharedRiskLinkGroup(252);
		SharedRiskLinkGroup group2 = new SharedRiskLinkGroup(252);
		SharedRiskLinkGroup group3 = new SharedRiskLinkGroup(987);
		SharedRiskLinkGroup group4 = group3;

		assertThat(group1.hashCode(), equalTo(group2.hashCode()));
		assertThat(group1.hashCode(), not(equalTo(group3.hashCode())));

		assertThat(group1, equalTo(group2));
		assertThat(group1, not(equalTo((group3))));
		assertThat(group3, equalTo(group4));
		assertNotNull(group1);
		assertThat(group1, not(new Object()));
	}

	@Test
	public void testCompareTo(){
		SharedRiskLinkGroup s1 = new SharedRiskLinkGroup(123);
		SharedRiskLinkGroup s2 = s1;
		SharedRiskLinkGroup s3 = new SharedRiskLinkGroup(158);
		assertTrue(s1.compareTo(s2) == 0);
		assertTrue(s1.compareTo(s3) < 0);
		assertTrue(s3.compareTo(s1) > 0);
	}

	@Test
	public void testEquals(){
		SharedRiskLinkGroup s1 = new SharedRiskLinkGroup(123);
		SharedRiskLinkGroup s2 = s1;
		SharedRiskLinkGroup s3 = new SharedRiskLinkGroup(158);
		assertFalse(s1.equals(null));
		assertFalse(s1.equals(new Object()));
		assertTrue(s1.equals(s2));
		assertFalse(s1.equals(s3));
	}

}
