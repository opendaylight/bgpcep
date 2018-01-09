/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.AbstractRouteEntryTest;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class BaseRouteEntryTest extends AbstractRouteEntryTest {

    private BaseSimpleRouteEntry testBARE;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testBaseSimpleRouteEntry() throws Exception {
        this.testBARE = new BaseSimpleRouteEntry();
        testWriteEmptyBestPath();
        testAddRouteSelectBestAndWriteOnDS();
        testRewriteSameRoute();
        testInitializePeerWithExistentRoute();
        testRemoveRoute();
    }

    private void testRemoveRoute() {
        Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(8, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaYii));
        this.testBARE.removeRoute(ROUTER_ID, REMOTE_PATH_ID);
        this.testBARE.selectBest(AS);
        this.testBARE.updateRoute(TABLES_KEY, this.peerPT, LOC_RIB_TARGET, this.ribSupport, this.tx, ROUTE_ID_PA);
        yiiCount = collectInfo();
        assertFalse(yiiCount.containsKey(this.routePaYii));
        assertFalse(yiiCount.containsKey(this.routeAddRiboutAttYii));
    }

    private void testInitializePeerWithExistentRoute() {
        this.testBARE.writeRoute(PEER_ID, ROUTE_ID_PA, PEER_YII2, this.peg, TABLES_KEY, this.peerPT, this.ribSupport,
                this.tx);
        assertEquals(8, this.yiichanges.size());
        Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(1, (long) yiiCount.get(this.routeRiboutYiiPeer2));
        assertEquals(1, (long) yiiCount.get(this.routeRiboutAttYiiPeer2));
    }

    private void testRewriteSameRoute() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport.routeAttributesIdentifier(), this.attributes);
        assertEquals(1, this.testBARE.getOffsets().size());
        assertFalse(this.testBARE.selectBest(AS));
    }

    private void testAddRouteSelectBestAndWriteOnDS() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport.routeAttributesIdentifier(), this.attributes);
        assertFalse(this.testBARE.getOffsets().isEmpty());
        this.testBARE.selectBest(AS);
        this.testBARE.updateRoute(TABLES_KEY, this.peerPT, LOC_RIB_TARGET, this.ribSupport, this.tx, ROUTE_ID_PA);
        Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(3, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaYii));
        assertEquals(1, (long) yiiCount.get(this.routeRiboutYii));
        assertEquals(1, (long) yiiCount.get(this.routeRiboutAttYii));
        this.testBARE.updateRoute(TABLES_KEY, this.peerPT, LOC_RIB_TARGET, this.ribSupport, this.tx,
                ROUTE_ID_PA_ADD_PATH);
        yiiCount = collectInfo();
        assertEquals(6, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaAddPathYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutAttYii));
    }

    private void testWriteEmptyBestPath() {
        this.testBARE.writeRoute(PEER_ID, ROUTE_ID_PA, PEER_YII2, this.peg, TABLES_KEY, this.peerPT, this.ribSupport,
                this.tx);
        assertEquals(0, this.yiichanges.size());
    }
}
