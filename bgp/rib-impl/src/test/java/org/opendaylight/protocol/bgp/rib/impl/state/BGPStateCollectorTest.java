/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.yangtools.concepts.Registration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BGPStateCollectorTest {
    @Mock
    private BGPRibStateProvider bgpribStateProvider;
    @Mock
    private BGPPeerStateProvider bgpPeerStateProvider;

    @Test
    public void getRibStatsTest() {
        doReturn(mock(BGPPeerState.class)).when(this.bgpPeerStateProvider).getPeerState();
        doReturn(mock(BGPRibState.class)).when(this.bgpribStateProvider).getRIBState();
        final BGPStateCollector collector = new BGPStateCollector();

        final Registration ribStateReg = collector.register(this.bgpribStateProvider);
        final Registration peerStateReg = collector.register(this.bgpPeerStateProvider);
        assertFalse(collector.getRibStats().isEmpty());
        assertFalse(collector.getPeerStats().isEmpty());

        ribStateReg.close();
        peerStateReg.close();
        assertEquals(List.of(), collector.getRibStats());
        assertEquals(List.of(), collector.getPeerStats());
    }

    @Test
    public void getRibStatsEmptyPeerTest() {
        doReturn(mock(BGPRibState.class)).when(this.bgpribStateProvider).getRIBState();
        doReturn(null).when(this.bgpPeerStateProvider).getPeerState();
        final BGPStateCollector collector = new BGPStateCollector();

        collector.register(this.bgpribStateProvider);
        collector.register(this.bgpPeerStateProvider);
        assertFalse(collector.getRibStats().isEmpty());
        assertTrue(collector.getPeerStats().isEmpty());
    }

    @Test
    public void getRibStatsEmptyRibTest() {
        doReturn(null).when(this.bgpribStateProvider).getRIBState();
        doReturn(null).when(this.bgpPeerStateProvider).getPeerState();
        final BGPStateCollector collector = new BGPStateCollector();
        assertTrue(collector.getRibStats().isEmpty());
        assertTrue(collector.getPeerStats().isEmpty());

        collector.register(this.bgpribStateProvider);
        collector.register(this.bgpPeerStateProvider);
        assertTrue(collector.getRibStats().isEmpty());
        assertTrue(collector.getPeerStats().isEmpty());
    }
}