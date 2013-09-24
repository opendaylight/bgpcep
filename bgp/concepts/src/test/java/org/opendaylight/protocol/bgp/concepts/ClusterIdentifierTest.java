/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

public class ClusterIdentifierTest {

	@Test
	public void testClusterIdentifier() {
		final ClusterIdentifier id = new ClusterIdentifier(new byte[] { 13, 14, 15, 16 });
		// FIXME: uncomment, once the generated code has length precondition
		// try {
		// new ClusterIdentifier(new byte[] { 5, 6 });
		// fail("Cluster ID is invalid!");
		// } catch (final IllegalArgumentException e) {
		// assertEquals("Invalid Cluster ID", e.getMessage());
		// }

		final ClusterIdentifier id1 = new ClusterIdentifier(new byte[] { 13, 14, 15, 16 });

		// FIXME: BUG-80 : uncomment, once it's done
		// assertEquals(id1.toString(), id.toString());

		assertArrayEquals(id1.getValue(), new byte[] { 13, 14, 15, 16 });
	}
}
