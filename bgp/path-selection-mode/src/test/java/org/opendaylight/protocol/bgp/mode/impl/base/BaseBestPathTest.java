/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

class BaseBestPathTest {
    private static final RouterId ROUTER_ID = RouterId.forAddress("127.0.0.1");
    private static final PeerId PEER_ID = new PeerId("bgp://127.0.0.1");

    private BaseBestPath baseBestPath;
    private BaseBestPath baseBestPathCopy;

    @BeforeEach
    void setUp() {
        final var selector = new BasePathSelector(20L);
        selector.processPath(BasePathSelectorTest.ROUTER_ID2,
            BasePathSelectorTest.createStateFromPrefMedOriginASPath().build());
        baseBestPath = selector.result();
        baseBestPathCopy = selector.result();
    }

    @Test
    void testGetRouterId() {
        assertEquals(ROUTER_ID, baseBestPath.getRouterId());
    }

    @Test
    void testGetPeerId() {
        assertEquals(PEER_ID, baseBestPath.getPeerId());
    }

    @Test
    void testGetPathId() {
        assertEquals(NON_PATH_ID_VALUE.toJava(), baseBestPath.getPathId());
    }

    @Test
    void testHashCodeAndEqual() {
        new EqualsTester().addEqualityGroup(baseBestPath, baseBestPathCopy).testEquals();
        assertEquals(baseBestPath.hashCode(), baseBestPathCopy.hashCode());
    }

    @Test
    void testToString() {
        assertEquals(baseBestPath.toString(), baseBestPathCopy.toString());
    }
}
