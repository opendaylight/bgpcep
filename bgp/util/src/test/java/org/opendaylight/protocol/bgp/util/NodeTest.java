/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPOrigin;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.util.BGPNodeImpl;

import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;

public class NodeTest {

	@Test
	public void testNodeImpl() {
		final BGPNodeImpl node1 = new BGPNodeImpl(new BaseBGPObjectState(BGPOrigin.INCOMPLETE, null), null, NetworkNodeState.EMPTY);
		final BGPNodeImpl node2 = new BGPNodeImpl(new BaseBGPObjectState(BGPOrigin.EGP, null), null, NetworkNodeState.EMPTY);

		assertFalse(node1.equals(node2));
		assertNotSame(node1.hashCode(), node2.hashCode());
		assertEquals(node1.toString(), node1.toString());
		assertNull(node1.currentState().getAggregator());
		assertEquals(node1.currentState().getOrigin(), BGPOrigin.INCOMPLETE);
		assertEquals(node1.getNodeIdentifier(), node2.getNodeIdentifier());
		assertEquals(node2, new BGPNodeImpl(new BaseBGPObjectState(BGPOrigin.EGP, null), null, NetworkNodeState.EMPTY));
	}
}
