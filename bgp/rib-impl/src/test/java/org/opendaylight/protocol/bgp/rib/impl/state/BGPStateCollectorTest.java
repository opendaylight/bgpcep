/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.yangtools.concepts.Registration;

@ExtendWith(MockitoExtension.class)
class BGPStateCollectorTest {
    @Mock
    private BGPRibStateProvider bgpribStateProvider;
    @Mock
    private BGPPeerStateProvider bgpPeerStateProvider;

    private final BGPStateCollector collector = new BGPStateCollector();

    @Test
    void getRibStatsTest() {
        assertEquals(List.of(), collector.getRibStats());
        assertEquals(List.of(), collector.getPeerStats());

        doReturn(mock(BGPPeerState.class)).when(bgpPeerStateProvider).getPeerState();
        doReturn(mock(BGPRibState.class)).when(bgpribStateProvider).getRIBState();
        try (Registration ribStateReg = collector.register(bgpribStateProvider)) {
            try (Registration peerStateReg = collector.register(bgpPeerStateProvider)) {
                assertEquals(1, collector.getRibStats().size());
                assertEquals(1, collector.getPeerStats().size());
            }
        }

        assertEquals(List.of(), collector.getRibStats());
        assertEquals(List.of(), collector.getPeerStats());
    }

    @Test
    void getRibStatsEmptyPeerTest() {
        doReturn(null).when(bgpPeerStateProvider).getPeerState();
        try (Registration peerStateReg = collector.register(bgpPeerStateProvider)) {
            assertEquals(List.of(), collector.getPeerStats());
        }
    }

    @Test
    void getRibStatsEmptyRibTest() {
        // FIXME: this is weird, getRIBState() specifies @NonNull return
        doReturn(null).when(bgpribStateProvider).getRIBState();
        try (Registration ribStateReg = collector.register(bgpribStateProvider)) {
            assertEquals(List.of(), collector.getRibStats());
        }
    }
}
