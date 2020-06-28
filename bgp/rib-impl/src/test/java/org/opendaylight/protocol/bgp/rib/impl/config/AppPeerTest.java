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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborPeerGroupConfigBuilder;

public class AppPeerTest extends AbstractConfig {
    private static final AppPeer APP_PEER = new AppPeer();
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
    public void testAppPeer() {
        APP_PEER.start(this.rib, this.neighbor, null, this.peerGroupLoader, this.tableTypeRegistry);
        Mockito.verify(this.rib).getYangRibId();
        Mockito.verify(this.rib).getService();
        Mockito.verify(this.rib).createPeerDOMChain(any(DOMTransactionChainListener.class));
        Mockito.verify(this.rib, times(1)).getLocalTablesKeys();

        APP_PEER.instantiateServiceInstance();
        Mockito.verify(this.rib, times(3)).getYangRibId();
        Mockito.verify(this.rib, times(2)).getRibSupportContext();
        Mockito.verify(this.rib, times(2)).getLocalTablesKeys();
        Mockito.verify(this.rib, times(2)).createPeerDOMChain(any(DOMTransactionChainListener.class));
        Mockito.verify(this.domTx).newWriteOnlyTransaction();

        APP_PEER.closeServiceInstance();
        Mockito.verify(this.domTx, times(2)).close();
        APP_PEER.close();

        APP_PEER.restart(this.rib, null, this.peerGroupLoader, this.tableTypeRegistry);
        APP_PEER.instantiateServiceInstance();
        Mockito.verify(this.rib, times(6)).getYangRibId();
        Mockito.verify(this.rib, times(4)).getService();
        Mockito.verify(this.rib, times(4)).createPeerDOMChain(any(DOMTransactionChainListener.class));
        Mockito.verify(this.listener, times(2)).close();

        assertTrue(APP_PEER.containsEqualConfiguration(this.neighbor));
        assertFalse(APP_PEER.containsEqualConfiguration(new NeighborBuilder()
                .setNeighborAddress(new IpAddress(new Ipv4Address("127.0.0.2"))).build()));
        APP_PEER.closeServiceInstance();
        Mockito.verify(this.domTx, times(4)).close();

        APP_PEER.instantiateServiceInstance();
        Mockito.verify(this.rib, times(6)).createPeerDOMChain(any(DOMTransactionChainListener.class));
        APP_PEER.closeServiceInstance();
        Mockito.verify(this.domTx, times(6)).close();
        APP_PEER.close();
    }
}
