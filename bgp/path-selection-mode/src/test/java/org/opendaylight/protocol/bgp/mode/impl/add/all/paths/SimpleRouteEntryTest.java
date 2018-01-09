/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.AbstractRouteEntryTest;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class SimpleRouteEntryTest extends AbstractRouteEntryTest {
    private SimpleRouteEntry testBARE;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testSimpleRouteEntry() throws Exception {
        this.testBARE = (SimpleRouteEntry) new AllPathSelection().createRouteEntry(false);
        testAddRouteSelectBestAndWriteOnDS();
        testRemoveRoute();
    }

    private void testAddRouteSelectBestAndWriteOnDS() {
        this.testBARE.addRoute(ROUTER_ID, REMOTE_PATH_ID, this.ribSupport
                .routeAttributesIdentifier(), this.attributes);
        assertFalse(this.testBARE.isEmpty());
        assertTrue(this.testBARE.selectBest(AS));
        /** Add AddPath Route **/
        this.testBARE.updateRoute(TABLES_KEY, this.peerPT, LOC_RIB_TARGET, this.ribSupport,
                this.tx, ROUTE_ID_PA_ADD_PATH);
        final Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(3, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaAddPathYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutYii));
        assertEquals(1, (long) yiiCount.get(this.routeAddRiboutAttYii));
    }

    private void testRemoveRoute() {
        Map<YangInstanceIdentifier, Long> yiiCount = collectInfo();
        assertEquals(3, yiiCount.size());
        assertEquals(1, (long) yiiCount.get(this.routePaAddPathYii));
        assertTrue(this.testBARE.removeRoute(ROUTER_ID, REMOTE_PATH_ID));
        assertTrue(this.testBARE.selectBest(AS));
        this.testBARE.updateRoute(TABLES_KEY, this.peerPT, LOC_RIB_TARGET, this.ribSupport,
                this.tx, ROUTE_ID_PA_ADD_PATH);
        yiiCount = collectInfo();
        assertEquals(0, yiiCount.size());
        assertFalse(yiiCount.containsKey(this.routePaAddPathYii));
        assertFalse(yiiCount.containsKey(this.routeAddRiboutYii));
    }
}
