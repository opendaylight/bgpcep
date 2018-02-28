/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.AbstractRouteEntryTest;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class SimpleRouteEntryTest extends AbstractRouteEntryTest {
    private static final long N_PATHS = 2;
    private SimpleRouteEntry testBARE;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testSimpleRouteEntry() throws Exception {
        this.testBARE = (SimpleRouteEntry) new AddPathBestNPathSelection(N_PATHS, this.peerTracker)
                .createRouteEntry(false);
        testWriteEmptyBestPath();
        testAddRouteSelectBestAndWriteOnDS();
        testRewriteSameRoute();
        testInitializePeerWithExistentRoute();
        testRemoveRoute();
    }

    /**
     * Add non Add Path Route.
     */
    @Test(expected = NullPointerException.class)
    public void testAddRouteSelectBestAndWriteOnDSs() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport.routeAttributesIdentifier(), this.attributes);
    }

    private void testWriteEmptyBestPath() {
        doReturn(ROUTE_ID_PA).when(this.entryInfo).getRouteId();

        this.testBARE.initializeBestPaths(this.entryDep, this.entryInfo, this.peg, this.tx);
        assertEquals(0, this.yiichanges.size());
    }

    /**
     * Add AddPath Route.
     */
    private void testAddRouteSelectBestAndWriteOnDS() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport.routeAttributesIdentifier(),
                this.attributes);
        assertFalse(this.testBARE.isEmpty());
        assertTrue(this.testBARE.selectBest(AS));
        this.testBARE.updateBestPaths(this.entryDep, ROUTE_ID_PA_ADD_PATH, this.tx);
        final Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(3, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaAddPathYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutAttYii));
    }

    private void testRewriteSameRoute() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport.routeAttributesIdentifier(), this.attributes);
        assertFalse(this.testBARE.selectBest(AS));
    }

    private void testInitializePeerWithExistentRoute() {
        assertEquals(3, this.yiichanges.size());
        doReturn(ROUTE_ID_PA_ADD_PATH).when(this.entryInfo).getRouteId();

        this.testBARE.initializeBestPaths(this.entryDep, this.entryInfo, this.peg, this.tx);
        assertEquals(5, this.yiichanges.size());
        final Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutYiiPeer2));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutYiiPeer2));
    }

    private void testRemoveRoute() {
        Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(5, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaAddPathYii));
        assertTrue(this.testBARE.removeRoute(ROUTER_ID, REMOTE_PATH_ID));
        assertTrue(this.testBARE.selectBest(AS));
        this.testBARE.updateBestPaths(this.entryDep, ROUTE_ID_PA_ADD_PATH, this.tx);
        yiiCount = collectInfo();
        assertEquals(2, yiiCount.size());
        assertFalse(yiiCount.containsKey(this.routePaAddPathYii));
        assertFalse(yiiCount.containsKey(this.routeAddRiboutYii));
    }
}
