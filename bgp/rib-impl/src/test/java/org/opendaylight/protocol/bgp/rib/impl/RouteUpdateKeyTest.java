/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;

public class RouteUpdateKeyTest {
    private static final UnsignedInteger PEER_ID = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.1"));
    private static final UnsignedInteger PEER_ID_2 = RouterIds.routerIdForPeerId(new PeerId("bgp://127.0.0.2"));
    private static final Ipv4RouteKey NIWP_PEER = new Ipv4RouteKey(new PathId(1L),
            new Ipv4Prefix("0.0.0.0/0"));
    private static final Ipv4RouteKey NIWP_PEER2 = new Ipv4RouteKey(new PathId(1L),
            new Ipv4Prefix("1.1.1.1/24"));

    @Test
    public void testRouteUpdateKey() {
        final RouteUpdateKey rk = new RouteUpdateKey(PEER_ID, NIWP_PEER);
        assertEquals(PEER_ID, rk.getPeerId());
        assertEquals(NIWP_PEER, rk.getRouteId());
        assertTrue(rk.equals(new RouteUpdateKey(PEER_ID, NIWP_PEER)));
        assertTrue(rk.equals(rk));
        assertFalse(rk.equals(null));
        assertFalse(rk.equals(new RouteUpdateKey(PEER_ID_2, NIWP_PEER)));
        assertFalse(rk.equals(new RouteUpdateKey(PEER_ID_2, NIWP_PEER2)));
    }
}