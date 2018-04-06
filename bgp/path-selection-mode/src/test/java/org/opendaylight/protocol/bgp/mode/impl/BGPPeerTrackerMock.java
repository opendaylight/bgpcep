/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class BGPPeerTrackerMock extends AbstractConcurrentDataBrokerTest {
    protected static final PeerId PEER_ID = new PeerId("bgp://42.42.42.42");
    protected static final PeerId PEER_ID2 = new PeerId("bgp://43.43.43.43");
    protected static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    @Mock
    protected BGPPeerTracker peerTracker;
    @Mock
    protected Peer peerMock;
    @Mock
    protected Peer peerMock2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockPeerTracker();
    }

    private void mockPeerTracker() {
        final PeerId pId = new PeerId("bgp://0.0.0.1");
        doReturn(this.peerMock).when(this.peerTracker).getPeer(eq(pId));
        doReturn(this.peerMock).when(this.peerTracker).getPeer(eq(PEER_ID));
        doReturn(true).when(this.peerMock).supportsTable(Mockito.eq(TABLES_KEY));
        doReturn(PeerRole.Ibgp).when(this.peerMock).getRole();
        doReturn(this.peerMock2).when(this.peerTracker).getPeer(eq(PEER_ID2));
        doReturn(false).when(this.peerMock2).supportsTable(Mockito.eq(TABLES_KEY));
    }
}
