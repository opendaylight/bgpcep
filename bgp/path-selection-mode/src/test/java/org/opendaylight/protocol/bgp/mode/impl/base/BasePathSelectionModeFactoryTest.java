/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.base;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;

public class BasePathSelectionModeFactoryTest {

    @Mock
    private BGPPeerTracker peerTracker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateBestPathSelectionStrategy() throws Exception {
        final PathSelectionMode psm = BasePathSelectionModeFactory.createBestPathSelectionStrategy(this.peerTracker);
        Assert.assertTrue(psm.createRouteEntry(true) instanceof BaseComplexRouteEntry);
        Assert.assertTrue(psm.createRouteEntry(false) instanceof BaseSimpleRouteEntry);
    }
}