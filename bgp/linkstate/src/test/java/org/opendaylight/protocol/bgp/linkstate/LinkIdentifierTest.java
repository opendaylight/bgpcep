/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4;

public class LinkIdentifierTest {

	@Test
	public void test() {

		final LinkAnchor a = new LinkAnchor(NodeIdentifierFactory.localIdentifier(new IPv4RouterIdentifier(IPv4.FAMILY.addressForString("12.51.8.4"))),
				IPv4InterfaceIdentifier.forString("12.51.8.4"));

		final LinkAnchor b = new LinkAnchor(NodeIdentifierFactory.localIdentifier(new IPv4RouterIdentifier(IPv4.FAMILY.addressForString("12.51.8.10"))),
				IPv4InterfaceIdentifier.forString("12.51.8.10"));

		final LinkIdentifier l1 = new LinkIdentifier(new TopologyIdentifier(5), a, b);

		final LinkIdentifier l11 = new LinkIdentifier(new TopologyIdentifier(5), a, b);

		final LinkIdentifier l2 = new LinkIdentifier(new TopologyIdentifier(5), b, a);

		assertNotSame(l1, l2);

		assertEquals(l1, l11);

		assertNotSame(l1.getLocalAnchor(), l2.getLocalAnchor());

		assertEquals(l1.getLocalAnchor(), l2.getRemoteAnchor());

		assertNotNull(l1.getTopology());

		assertEquals(l1.hashCode(), l11.hashCode());

		assertEquals(l1.toString(), l11.toString());
	}
}
