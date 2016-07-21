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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

public class RouterIdsTest {

    private final static String ADDRESS = "42.42.42.42";
    private final UnsignedInteger unsignedRouterId = UnsignedInteger.valueOf(707406378);
    private final PeerId peerID = new PeerId("bgp://" + ADDRESS);

    @Test
    public void testRouterIdForAddress() throws Exception {
        assertEquals(unsignedRouterId, RouterIds.routerIdForAddress(ADDRESS));
    }

    @Test
    public void testRouterIdForPeerId() throws Exception {
        assertEquals(unsignedRouterId, RouterIds.routerIdForPeerId(peerID));
    }

    @Test
    public void testCreatePeerId() throws Exception {
        assertEquals(peerID, RouterIds.createPeerId(new Ipv4Address(ADDRESS)));
    }

    @Test
    public void testCreatePeerId1() throws Exception {
        assertEquals(peerID, RouterIds.createPeerId(unsignedRouterId));
    }
}