/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;

public class BGPStateCollectorImplTest {
    @Mock
    private BGPRibStateConsumer bgpribStateConsumer;
    @Mock
    private BGPPeerStateConsumer bgpPeerStateConsumer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(BGPPeerState.class)).when(this.bgpPeerStateConsumer).getPeerState();
        doReturn(mock(BGPRibState.class)).when(this.bgpribStateConsumer).getRIBState();
    }

    @Test
    public void getRibStats() throws Exception {
        final BGPStateCollectorImpl collector = new BGPStateCollectorImpl();
        final BGPRibStateConsumer ribStateConsumerNull = null;
        collector.bind(ribStateConsumerNull);
        assertTrue(collector.getRibStats().isEmpty());

        final BGPPeerStateConsumer peerStateConsumerNull = null;
        collector.bind(peerStateConsumerNull);
        assertTrue(collector.getPeerStats().isEmpty());

        collector.bind(this.bgpribStateConsumer);
        collector.bind(this.bgpPeerStateConsumer);
        assertFalse(collector.getRibStats().isEmpty());
        assertFalse(collector.getPeerStats().isEmpty());

        collector.unbind(this.bgpribStateConsumer);
        collector.unbind(this.bgpPeerStateConsumer);
        assertTrue(collector.getRibStats().isEmpty());
        assertTrue(collector.getPeerStats().isEmpty());
    }
}