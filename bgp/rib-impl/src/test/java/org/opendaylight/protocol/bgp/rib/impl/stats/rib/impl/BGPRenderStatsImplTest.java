/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocRibRouteTable;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPRenderStatsImplTest {
    private static final BgpId BGP_ID = new BgpId("127.0.0.1");
    private static final RibId RIB_ID = new RibId("test-rib");
    private static final ClusterIdentifier CLUSTER_ID = new ClusterIdentifier("192.168.1.2");
    private static final AsNumber AS = new AsNumber(0x10L);
    private static final ZeroBasedCounter32 COUTER = new ZeroBasedCounter32(0L);
    private static final ZeroBasedCounter32 COUTER_ONE_ROUTE = new ZeroBasedCounter32(1L);
    private final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Mock
    private BGPRIBState bgpGlobalState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void getBgpRenderState() throws Exception {
        final BGPRenderStatsImpl render = new BGPRenderStatsImpl(BGP_ID, RIB_ID, AS, CLUSTER_ID, this.bgpGlobalState,
            Collections.singleton(this.tk));

        final BgpRenderState renderStateExpected = new BgpRenderState();
        renderStateExpected.setRibId(RIB_ID);
        renderStateExpected.setBgpRibId(BGP_ID);
        renderStateExpected.setClusterId(CLUSTER_ID);
        renderStateExpected.setLocalAs(AS);
        renderStateExpected.setConfiguredPeerCount(COUTER);
        renderStateExpected.setConnectedPeerCount(COUTER);
        final LocRibRouteTable locRibTable = new LocRibRouteTable();
        locRibTable.setAfi(new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString()));
        locRibTable.setSafi(new IdentityAttributeRef(UnicastSubsequentAddressFamily.QNAME.toString()));
        locRibTable.setRoutesCount(COUTER);
        final List<LocRibRouteTable> locRibRouteTableList = Collections.singletonList(locRibTable);
        renderStateExpected.setLocRibRouteTable(locRibRouteTableList);
        renderStateExpected.setLocRibRoutesCount(COUTER);
        doReturn(0L).when(this.bgpGlobalState).getPathCount(eq(this.tk));

        assertEquals(renderStateExpected, render.getBgpRenderState());
        doReturn(1L).when(this.bgpGlobalState).getPathCount(eq(this.tk));
        locRibTable.setRoutesCount(COUTER_ONE_ROUTE);
        renderStateExpected.setLocRibRoutesCount(COUTER_ONE_ROUTE);
        assertEquals(renderStateExpected, render.getBgpRenderState());
        render.getConfiguredPeerCounter().increment();
        assertEquals(1L, render.getConfiguredPeerCounter().longValue());
        render.getConnectedPeerCounter().increment();
        assertEquals(1L, render.getConnectedPeerCounter().longValue());
    }
}