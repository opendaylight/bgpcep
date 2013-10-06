/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.AreaIdentifier;

public class AreaIdentifierTest {

	private AreaIdentifier identifier;

	@Before
	public void init() {
		final byte[] id = new byte[] {10,0,0,1};
		this.identifier = new AreaIdentifier(id);
	}

	@Test
	public void testGetBytes() {
		final byte[] id = new byte[] {10,0,0,1};
		assertArrayEquals(id, this.identifier.getValue());
	}

	@Test
	public void testToString(){
		final byte[] id1 = new byte[] {10,0,0,1};
		final AreaIdentifier l1 = new AreaIdentifier(id1);
		final String s1 = "AreaIdentifier [_value=[10, 0, 0, 1]]";
		assertEquals(s1, l1.toString());
	}
}
