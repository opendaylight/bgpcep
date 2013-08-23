/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPUpdateSynchronized;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.concepts.ASNumber;

public class ApiTest {

	@Test
	public void testBGPSessionPreferences() {
		final BGPSessionPreferences sp = new BGPSessionPreferences(new ASNumber(58), (short) 5, null, null);
		assertNull(sp.getBgpId());
		assertEquals((short) 5, sp.getHoldTime());
		assertEquals(58, sp.getMyAs().getAsn());
	}

	@Test
	public void testBGPUpdateSynchronized() {
		final BGPTableType tt = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Linkstate);
		final BGPUpdateSynchronized update = new BGPUpdateSynchronizedImpl(tt);
		assertEquals(tt, update.getTableType());
	}
}
