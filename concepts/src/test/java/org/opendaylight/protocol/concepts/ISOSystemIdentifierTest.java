/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import org.junit.Assert;
import org.junit.Test;

public class ISOSystemIdentifierTest {

	@Test(expected=IllegalArgumentException.class)
	public void testISOSystemIdentifier() {
		byte[] b = new byte[]{10,12,127,0,9,1,1};
		new ISOSystemIdentifier(b);
	}

	@Test
	public void testGetBytes() {
		byte[] b = new byte[]{10,12,127,0,9,1};
		ISOSystemIdentifier id = new ISOSystemIdentifier(b);
		Assert.assertArrayEquals(new byte[]{10,12,127,0,9,1}, id.getBytes());
	}

}
