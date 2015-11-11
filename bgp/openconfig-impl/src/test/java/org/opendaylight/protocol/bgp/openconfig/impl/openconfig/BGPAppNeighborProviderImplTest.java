/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpApplicationPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;

public class BGPAppNeighborProviderImplTest {

    private BGPAppNeighborProviderImpl neighborProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Neighbor> configHolder = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        neighborProvider = new BGPAppNeighborProviderImpl(txChain, stateHolders);
    }

    @Test
    public void testCreateModuleKey() {
        assertEquals(new ModuleKey("instanceName", BgpApplicationPeer.class), neighborProvider.createModuleKey("instanceName"));
    }

    @Test
    public void testGetInstanceConfigurationType() {
        assertEquals(BGPAppPeerInstanceConfiguration.class, neighborProvider.getInstanceConfigurationType());
    }

    @Test
    public void testApply() {
        final Neighbor neighbor = neighborProvider.apply(new BGPAppPeerInstanceConfiguration(new InstanceConfigurationIdentifier("instanceName"), "app-rib", new Ipv4Address("1.2.3.4")));
        final Neighbor expectedNeighbor = new NeighborBuilder()
            .setNeighborAddress(new IpAddress(new Ipv4Address("1.2.3.4")))
            .setKey(new NeighborKey(new IpAddress(new Ipv4Address("1.2.3.4"))))
            .setConfig(new ConfigBuilder().addAugmentation(Config1.class, new Config1Builder().setPeerGroup("application-peers").build()).build())
            .build();
        assertEquals(expectedNeighbor, neighbor);
    }

}
