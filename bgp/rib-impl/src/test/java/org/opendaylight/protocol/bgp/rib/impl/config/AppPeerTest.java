/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPStateCollector;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborPeerGroupConfigBuilder;

public class AppPeerTest extends AbstractConfig {
    private final AppPeer appPeer = new AppPeer(new BGPStateCollector());

    private final Neighbor neighbor = new NeighborBuilder()
        .setConfig(new ConfigBuilder()
            .addAugmentation(new NeighborPeerGroupConfigBuilder()
                .setPeerGroup(OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME)
                .build())
            .build())
        .setNeighborAddress(new IpAddress(new Ipv4Address("127.0.0.1"))).build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testAppPeer() throws ExecutionException, InterruptedException {
        appPeer.start(rib, neighbor, null, peerGroupLoader, tableTypeRegistry);
        verify(rib).getYangRibId();
        verify(rib).getService();
        verify(rib).createPeerDOMChain();
        verify(rib, times(1)).getLocalTablesKeys();

        appPeer.instantiateServiceInstance();
        verify(rib, times(3)).getYangRibId();
        verify(rib, times(2)).getRibSupportContext();
        verify(rib, times(2)).getLocalTablesKeys();
        verify(rib, times(2)).createPeerDOMChain();
        verify(domTx).newWriteOnlyTransaction();

        appPeer.closeServiceInstance();
        verify(domTx, times(2)).close();
        appPeer.stop().get();

        appPeer.start(rib, appPeer.getCurrentConfiguration(), null, peerGroupLoader, tableTypeRegistry);
        appPeer.instantiateServiceInstance();
        verify(rib, times(6)).getYangRibId();
        verify(rib, times(4)).getService();
        verify(rib, times(4)).createPeerDOMChain();
        verify(listener, times(2)).close();

        assertTrue(appPeer.containsEqualConfiguration(neighbor));
        assertFalse(appPeer.containsEqualConfiguration(new NeighborBuilder()
                .setNeighborAddress(new IpAddress(new Ipv4Address("127.0.0.2")))
                .build()));
        appPeer.closeServiceInstance();
        verify(domTx, times(4)).close();

        appPeer.instantiateServiceInstance();
        verify(rib, times(6)).createPeerDOMChain();
        appPeer.closeServiceInstance();
        verify(domTx, times(6)).close();
        appPeer.stop().get();
    }
}
