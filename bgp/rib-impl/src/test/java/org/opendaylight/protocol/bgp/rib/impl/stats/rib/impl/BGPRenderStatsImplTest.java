/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocRibRouteTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPRenderStatsImplTest {
    private final static BgpId BGP_ID = new BgpId("127.0.0.1");
    private static final RibId RIB_ID = new RibId("test-rib");
    private static final ClusterIdentifier CLUSTER_ID = new ClusterIdentifier("192.168.1.2");
    private static final AsNumber AS = new AsNumber(0x10L);
    private static final ZeroBasedCounter32 COUTER = new ZeroBasedCounter32(0L);

    @Test
    public void getBgpRenderState() throws Exception {
        final BGPRenderStatsImpl render = new BGPRenderStatsImpl(BGP_ID, RIB_ID, AS, CLUSTER_ID);

        final BgpRenderState renderStateExpected = new BgpRenderState();
        renderStateExpected.setRibId(RIB_ID);
        renderStateExpected.setBgpRibId(BGP_ID);
        renderStateExpected.setClusterId(CLUSTER_ID);
        renderStateExpected.setLocalAs(AS);
        renderStateExpected.setConfiguredPeerCount(COUTER);
        renderStateExpected.setConnectedPeerCount(COUTER);
        final List<LocRibRouteTable> locRibRouteTableList = new ArrayList<>();
        renderStateExpected.setLocRibRouteTable(locRibRouteTableList);
        renderStateExpected.setLocRibRoutesCount(COUTER);

        assertEquals(renderStateExpected, render.getBgpRenderState());
        assertEquals(1L, render.getLocRibRouteCounter().init(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)).increaseCount());
        assertEquals(1L, render.getConfiguredPeerCounter().increaseCount());
        assertEquals(1L, render.getConnectedPeerCounter().increaseCount());
    }
}