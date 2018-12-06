/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

public class RouteUpdateKeyTest {
    private static final RouterId PEER_ID = RouterId.forPeerId(new PeerId("bgp://127.0.0.1"));
    private static final RouterId PEER_ID_2 = RouterId.forPeerId(new PeerId("bgp://127.0.0.2"));
    private static final String PREFIX = "0.0.0.0/0";
    private static final String PREFIX_2 = "1.1.1.1/24";

    @Test
    public void testRouteUpdateKey() {
        final RouteUpdateKey rk = new RouteUpdateKey(PEER_ID, PREFIX);
        assertEquals(PEER_ID, rk.getPeerId());
        assertEquals(PREFIX, rk.getRouteId());
        assertEquals(rk, new RouteUpdateKey(PEER_ID, PREFIX));
        assertEquals(rk, rk);
        assertNotEquals(rk, new RouteUpdateKey(PEER_ID_2, PREFIX));
        assertNotEquals(rk, new RouteUpdateKey(PEER_ID_2, PREFIX_2));
    }
}