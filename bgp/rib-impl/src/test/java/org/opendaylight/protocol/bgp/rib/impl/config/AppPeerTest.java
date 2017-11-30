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
import static org.mockito.Matchers.any;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;

public class AppPeerTest extends AbstractConfig {
    private static final AppPeer APP_PEER = new AppPeer();
    private final Neighbor neighbor = new NeighborBuilder().setConfig(new ConfigBuilder().addAugmentation(Config2.class,
            new Config2Builder().setPeerGroup(OpenConfigMappingUtil.APPLICATION_PEER_GROUP_NAME).build()).build())
            .setNeighborAddress(new IpAddress(new Ipv4Address("127.0.0.1"))).build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testAppPeer() throws Exception {
        APP_PEER.start(this.rib, this.neighbor, this.tableTypeRegistry);
        Mockito.verify(this.rib).getYangRibId();
        Mockito.verify(this.rib).getService();
        Mockito.verify(this.rib).getRibIServiceGroupIdentifier();
        Mockito.verify(this.rib).registerClusterSingletonService(any(ClusterSingletonService.class));
        Mockito.verify(this.rib, times(1)).getLocalTablesKeys();

        this.singletonService.instantiateServiceInstance();
        Mockito.verify(this.rib, times(2)).getYangRibId();
        Mockito.verify(this.rib).getRibSupportContext();
        Mockito.verify(this.rib, times(2)).getLocalTablesKeys();
        Mockito.verify(this.domTx).newWriteOnlyTransaction();

        APP_PEER.close();
        Mockito.verify(this.singletonServiceRegistration).close();

        APP_PEER.restart(this.rib, this.tableTypeRegistry);
        this.singletonService.instantiateServiceInstance();
        Mockito.verify(this.rib, times(4)).getYangRibId();
        Mockito.verify(this.rib, times(4)).getService();
        Mockito.verify(this.rib, times(2)).getRibIServiceGroupIdentifier();
        Mockito.verify(this.rib, times(2))
                .registerClusterSingletonService(any(ClusterSingletonService.class));
        Mockito.verify(this.listener, times(2)).close();

        assertTrue(APP_PEER.containsEqualConfiguration(this.neighbor));
        assertFalse(APP_PEER.containsEqualConfiguration(new NeighborBuilder()
                .setNeighborAddress(new IpAddress(new Ipv4Address("127.0.0.2"))).build()));
        APP_PEER.close();
        Mockito.verify(this.singletonServiceRegistration, times(2)).close();
    }
}