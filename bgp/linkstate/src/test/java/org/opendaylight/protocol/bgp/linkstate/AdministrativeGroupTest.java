/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.hamcrest.core.IsNot.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class AdministrativeGroupTest {

	@Test
	public void testOverflows() {
		try {
			new AdministrativeGroup(-2);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
		try {
			new AdministrativeGroup(4294967296L);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
	}

	@Test
	public void testGetValue() {
		AdministrativeGroup ag = new AdministrativeGroup(12345);
		assertEquals(12345, ag.getValue());
	}

	@Test
	public void testHashCode() {
		AdministrativeGroup ag1 = new AdministrativeGroup(4321);
		AdministrativeGroup ag2 = new AdministrativeGroup(4321);
		assertEquals(ag1.hashCode(), ag2.hashCode());
	}

	@Test
	public void testEquals() {
		AdministrativeGroup ag1 = new AdministrativeGroup(4321);
		AdministrativeGroup ag2 = ag1;
		assertEquals(ag1, ag2);
		assertEquals(ag1, new AdministrativeGroup(4321));
		assertNotNull(ag1);
		assertThat(ag1, not(new Object()));
		assertThat(ag1, not(new AdministrativeGroup(5432)));
		assertFalse(ag1.equals(null));
		assertFalse(ag1.equals(new Object()));
	}

	@Test
	public void testCompareTo(){
		AdministrativeGroup a1 = new AdministrativeGroup(10);
		AdministrativeGroup a2 = new AdministrativeGroup(5);
		AdministrativeGroup a3 = new AdministrativeGroup(10);
		assertTrue(a1.compareTo(a2) > 0);
		assertTrue(a2.compareTo(a1) < 0);
		assertTrue(a1.compareTo(a3) == 0);
	}

}
