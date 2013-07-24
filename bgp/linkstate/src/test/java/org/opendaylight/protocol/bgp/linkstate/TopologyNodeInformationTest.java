/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.*;

import org.junit.Test;

public class TopologyNodeInformationTest {

	@Test
	public void test() {
		TopologyNodeInformation nodeInfo1 = new TopologyNodeInformation(true, true);
		TopologyNodeInformation nodeInfo2 = new TopologyNodeInformation(false, false);
		assertTrue(nodeInfo1.isAttached());
		assertTrue(nodeInfo1.isOverloaded());
		assertFalse(nodeInfo2.isAttached());
		assertFalse(nodeInfo2.isOverloaded());
	}

	@Test
	public void testEquals(){
		TopologyNodeInformation t1 =  new TopologyNodeInformation(true, true);
		TopologyNodeInformation t2 = t1;
		TopologyNodeInformation t3 = new TopologyNodeInformation(true, false);
		assertEquals(t1, t2);
		assertEquals(t1,  new TopologyNodeInformation(true, true));
		assertFalse(t1.equals(t3));
		assertFalse(t1.equals(null));
		assertFalse(t1.equals(new Object()));
	}

	@Test
	public void testHashCode(){
		TopologyNodeInformation t1 = new TopologyNodeInformation(true, false);
		TopologyNodeInformation t2 = new TopologyNodeInformation(true, false);
		assertEquals(t1.hashCode(), t2.hashCode());
	}

	@Test
	public void testToString(){
		TopologyNodeInformation t1 = new TopologyNodeInformation(true, false);
		String s = "TopologyNodeInformation [attached=" + t1.isAttached() + ", overloaded=" + t1.isOverloaded() + "]";
		assertEquals(s, t1.toString());
	}
}
