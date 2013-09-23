/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class ASSpecificExtendedCommunityTest {

	private final boolean transitive = true;
	private final int subType = 123;
	private final AsNumber globalAdmin = new AsNumber(429496729800L);
	private final byte[] localAdmin = new byte[] { 10, 0, 0, 1 };

	@Test
	public void testOverflows() {
		try {
			new ASSpecificExtendedCommunity(this.transitive, -2, this.globalAdmin, this.localAdmin);
			fail("Sub-type is negative!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Sub-Type", e.getMessage());
		}
		try {
			new ASSpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, new byte[] {});
			fail("Local Administrator has illegal length!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Local Administrator", e.getMessage());
		}
	}

	@Test
	public void testGetSubType() {
		final ASSpecificExtendedCommunity asSpecExCom = new ASSpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, this.localAdmin);
		assertEquals(123, asSpecExCom.getSubType());
		assertEquals(new AsNumber(429496729800L), asSpecExCom.getGlobalAdmin());
		assertArrayEquals(new byte[] { 10, 0, 0, 1 }, asSpecExCom.getLocalAdmin());

		final ASSpecificExtendedCommunity a1 = new ASSpecificExtendedCommunity(this.transitive, this.subType, this.globalAdmin, this.localAdmin);
		assertEquals(a1.toString(), asSpecExCom.toString());
	}
}
