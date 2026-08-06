/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

class RouterIdsTest {

    private final PeerId peerID = new PeerId("bgp://42.42.42.42");
    private final RouterId routerId = new RouterId(peerID);

    @Test
    void testRouterIdForAddress() throws Exception {
        assertEquals(this.routerId, RouterId.forAddress("42.42.42.42"));
    }

    @Test
    void testRouterIdForPeerId() throws Exception {
        assertEquals(this.routerId, RouterId.forPeerId(this.peerID));
    }

    @Test
    void testCreatePeerId() throws Exception {
        assertEquals(this.peerID, RouterIds.createPeerId(new Ipv4Address("42.42.42.42")));
    }

    @Test
    void testCreatePeerId1() throws Exception {
        assertSame(this.peerID, routerId.getPeerId());
    }
}
