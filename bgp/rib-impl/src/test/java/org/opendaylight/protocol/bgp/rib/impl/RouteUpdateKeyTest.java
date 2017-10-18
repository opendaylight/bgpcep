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
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.PEER_ID_QNAME;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class RouteUpdateKeyTest {
    private static final PeerId PEER_ID = new PeerId("127.0.0.1");
    private static final PeerId PEER_ID_2 = new PeerId("127.0.0.2");
    private static final NodeIdentifierWithPredicates NIWP_PEER = new NodeIdentifierWithPredicates(Peer.QNAME,
        ImmutableMap.of(PEER_ID_QNAME, PEER_ID.getValue()));

    @Test
    public void testRouteUpdateKey() {
        final RouteUpdateKey rk = new RouteUpdateKey(PEER_ID, NIWP_PEER);
        assertEquals(PEER_ID, rk.getPeerId());
        assertEquals(NIWP_PEER, rk.getRouteId());
        assertTrue(rk.equals(new RouteUpdateKey(PEER_ID, NIWP_PEER)));
        assertTrue(rk.equals(rk));
        assertFalse(rk.equals(null));
        assertFalse(rk.equals(new RouteUpdateKey(PEER_ID_2, NIWP_PEER)));
        assertFalse(rk.equals(new RouteUpdateKey(PEER_ID, new NodeIdentifierWithPredicates(Peer.QNAME,
            ImmutableMap.of(PEER_ID_QNAME, PEER_ID_2.getValue())))));
    }

}