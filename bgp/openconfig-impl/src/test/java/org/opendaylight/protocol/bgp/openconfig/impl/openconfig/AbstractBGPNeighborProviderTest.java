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
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractBGPNeighborProviderTest {

    private AbstractBGPNeighborProvider<InstanceConfiguration> absNeighborProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Neighbor> configHolder = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        absNeighborProvider = new AbstractBGPNeighborProvider<InstanceConfiguration>(txChain, stateHolders, Neighbor.class) {
            @Override
            public Neighbor apply(final InstanceConfiguration input) {
                return null;
            }
            @Override
            public ModuleKey createModuleKey(final String instanceName) {
                return null;
            }
            @Override
            public Class<InstanceConfiguration> getInstanceConfigurationType() {
                return null;
            }
        };
    }

    @Test
    public void testGetInstanceIdentifierNeighborKey() {
        assertEquals(InstanceIdentifier.create(Bgp.class).child(Neighbors.class).child(Neighbor.class, new NeighborKey(new IpAddress(new Ipv4Address("1.2.3.4")))),
                absNeighborProvider.getInstanceIdentifier(new NeighborKey(new IpAddress(new Ipv4Address("1.2.3.4")))));
    }

    @Test
    public void testKeyForConfigurationNeighbor() {
        assertEquals(new NeighborKey(new IpAddress(new Ipv4Address("1.2.3.4"))),
                absNeighborProvider.keyForConfiguration(new NeighborBuilder().setKey(new NeighborKey((new IpAddress(new Ipv4Address("1.2.3.4"))))).build()));
    }

}
