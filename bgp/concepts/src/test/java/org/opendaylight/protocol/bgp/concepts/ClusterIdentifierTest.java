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
import org.opendaylight.protocol.bgp.concepts.ClusterIdentifier;

public class ClusterIdentifierTest {

	@Test
	public void testClusterIdentifier() {
		final ClusterIdentifier id = new ClusterIdentifier(new byte[] { 13, 14, 15, 16 });
		try {
			new ClusterIdentifier(new byte[] { 5, 6 });
			fail("Cluster ID is invalid!");
		} catch (final IllegalArgumentException e) {
			assertEquals("Invalid Cluster ID", e.getMessage());
		}

		final ClusterIdentifier id1 = new ClusterIdentifier(new byte[] { 13, 14, 15, 16 });

		assertEquals(id1.toString(), id.toString());

		assertArrayEquals(id1.getBytes(), new byte[] { 13, 14, 15, 16 });
	}
}
