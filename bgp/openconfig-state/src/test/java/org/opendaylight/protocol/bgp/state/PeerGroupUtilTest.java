/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PeerGroupUtilTest {
    @Mock
    private BGPPeerState bgpPeerState;

    @Before
    public void setUp() {
        doReturn(null).when(bgpPeerState).getGroupId();
    }

    @Test
    public void testNoneGroup() {
        assertNull(PeerGroupUtil.buildPeerGroups(List.of(bgpPeerState)));
    }
}