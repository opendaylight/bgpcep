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

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.as.specific.extended.community.AsSpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.c.as.specific.extended.community.AsSpecificExtendedCommunityBuilder;

public class ASSpecificExtendedCommunityTest {

	private final boolean transitive = true;
	private final AsNumber globalAdmin = new AsNumber(429496729800L);

	@Test
	@Ignore
	// FIXME: length is not implemented
	public void testOverflows() {
		try {
			new AsSpecificExtendedCommunityBuilder().setTransitive(this.transitive).setGlobalAdministrator(this.globalAdmin).setLocalAdministrator(
					new byte[] {}).build();
			fail("Local Administrator has illegal length!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Local Administrator", e.getMessage());
		}
	}

	@Test
	public void testGetters() {
		final AsSpecificExtendedCommunity asSpecExCom = new AsSpecificExtendedCommunityBuilder().setTransitive(this.transitive).setGlobalAdministrator(
				this.globalAdmin).setLocalAdministrator(new byte[] { 10, 0, 0, 1 }).build();
		assertEquals(new AsNumber(429496729800L), asSpecExCom.getGlobalAdministrator());
		assertArrayEquals(new byte[] { 10, 0, 0, 1 }, asSpecExCom.getLocalAdministrator());
	}
}
