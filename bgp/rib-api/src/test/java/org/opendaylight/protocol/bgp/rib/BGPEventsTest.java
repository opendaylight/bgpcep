/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPRouteState;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpSubsequentAddressFamily;

public class BGPEventsTest {

	@Test
	public void testSynchronizedEvent() {
		final BGPTableType tt = new BGPTableType(BGPAddressFamily.IPv6, BgpSubsequentAddressFamily.MplsLabeledVpn);
		final RIBSynchronizedEvent event = new RIBSynchronizedEvent(tt);
		assertEquals(tt, event.getTable());
	}

	@Test
	public void testChangedEvent() {
		final RIBChangedEvent event = new RIBChangedEvent(Collections.<Prefix<?>, BGPRouteState<?>> emptyMap());
		assertNotNull(event.getLinks());
		assertNotNull(event.getNodes());
		assertNotNull(event.getPrefixes());
		assertNotNull(event.getRoutes());
	}
}
