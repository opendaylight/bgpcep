/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

public class BaseBestPathTest {
    private static final RouterId ROUTER_ID = RouterId.forAddress("127.0.0.1");
    private static final PeerId PEER_ID = new PeerId("bgp://127.0.0.1");
    private BaseBestPath baseBestPath;
    private BaseBestPath baseBestPathCopy;

    @Before
    public void setUp() {
        final BasePathSelector selector = new BasePathSelector(20L);
        selector.processPath(BasePathSelectorTest.ROUTER_ID2,
                BasePathSelectorTest.createStateFromPrefMedOriginASPath().build());
        this.baseBestPath = selector.result();
        this.baseBestPathCopy = selector.result();
    }

    @Test
    public void testGetRouterId() {
        assertEquals(ROUTER_ID, this.baseBestPath.getRouterId());
    }

    @Test
    public void testGetPeerId() {
        assertEquals(PEER_ID, this.baseBestPath.getPeerId());
    }

    @Test
    public void testGetPathId() {
        assertEquals(NON_PATH_ID_VALUE, this.baseBestPath.getPathId());
    }

    @Test
    public void testHashCodeAndEqual() {
        assertTrue(this.baseBestPath.equals(this.baseBestPathCopy)
                && this.baseBestPathCopy.equals(this.baseBestPath));
        assertEquals(this.baseBestPath.hashCode(), this.baseBestPathCopy.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(this.baseBestPath.toString(), this.baseBestPathCopy.toString());
    }
}