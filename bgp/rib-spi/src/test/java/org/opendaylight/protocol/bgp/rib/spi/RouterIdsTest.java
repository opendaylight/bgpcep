/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;

public class RouterIdsTest {

    UnsignedInteger unsignedRouterId = UnsignedInteger.valueOf(707406378);
    PeerId peerID = new PeerId("bgp://42.42.42.42");

    @Test
    public void testRouterIdForAddress() throws Exception {
        assertEquals(this.unsignedRouterId, RouterIds.routerIdForAddress("42.42.42.42"));
    }

    @Test
    public void testRouterIdForPeerId() throws Exception {
        assertEquals(this.unsignedRouterId, RouterIds.routerIdForPeerId(this.peerID));
    }

    @Test
    public void testCreatePeerId() throws Exception {
        assertEquals(this.peerID, RouterIds.createPeerId(new Ipv4Address("42.42.42.42")));
    }

    @Test
    public void testCreatePeerId1() throws Exception {
        assertEquals(this.peerID, RouterIds.createPeerId(this.unsignedRouterId));
    }
}