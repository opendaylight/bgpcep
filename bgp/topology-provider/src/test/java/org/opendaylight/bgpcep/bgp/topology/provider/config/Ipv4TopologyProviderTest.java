/*
 * Copyright (c) 2017 Inocybe Technologies, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.Ipv4ReachabilityTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.protocol.util.CheckUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4TopologyProviderTest extends AbstractBgpTopologyProviderTest {
    private Ipv4TopologyProvider ipv4TopologyProvider;
    @Before
    @Override
    public void setUp() {
        super.setUp();
        this.ipv4TopologyProvider = new Ipv4TopologyProvider(this.bgpTopologyDeployer);
    }

    @Test
    public void testCreatedIpv4TopologyProvider() throws Exception {
        assertNotNull(this.ipv4TopologyProvider);
    }

    @Test
    public void testTopologyTypeFilter() throws Exception {
        final TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(this.TEST_TOPOLOGY_ID);
        topologyBuilder.setKey(new TopologyKey(topologyBuilder.getTopologyId()));
        topologyBuilder.setTopologyTypes(Ipv4ReachabilityTopologyBuilder.IPV4_TOPOLOGY_TYPE);
        topologyBuilder.addAugmentation(Topology1.class, new Topology1Builder().setRibId(new RibId("test-rib")).build());
        final Topology topology = topologyBuilder.build();
        boolean result = this.ipv4TopologyProvider.topologyTypeFilter(this.topologyTypes1Builder.build());
        assertFalse(result);
        result = this.ipv4TopologyProvider.topologyTypeFilter(topology);
        assertTrue(result);
    }

    @Test
    public void testCreateTopologyBuilder() throws Exception {
        AbstractTopologyBuilder result = this.ipv4TopologyProvider.createTopologyBuilder(getDataBroker(), this.LOC_RIB_REF, this.TEST_TOPOLOGY_ID);
        assertNotNull(result);
        assertTrue(result instanceof Ipv4ReachabilityTopologyBuilder);
    }

    @Test
    public void testOnTopologyBuilderCreatedAndRemoved() throws Exception {
        final TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(this.TEST_TOPOLOGY_ID);
        topologyBuilder.setKey(new TopologyKey(topologyBuilder.getTopologyId()));
        topologyBuilder.setTopologyTypes(Ipv4ReachabilityTopologyBuilder.IPV4_TOPOLOGY_TYPE);
        topologyBuilder.addAugmentation(Topology1.class, new Topology1Builder().setRibId(new RibId("test-rib")).build());
        final Topology topology = topologyBuilder.build();
        ipv4TopologyProvider.onTopologyBuilderCreated(topology, null);
        // CheckUtil.checkPresentOperational(getDataBroker(), InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(topology.getAugmentation(Topology1.class).getRibId())));
        Field field = ipv4TopologyProvider.getClass().getSuperclass().getDeclaredField("topologyBuilders");
        field.setAccessible(true);
        Map<TopologyId, TopologyReferenceSingletonService> result = (Map<TopologyId, TopologyReferenceSingletonService>) field.get(ipv4TopologyProvider);
        assertTrue(result.containsKey(this.TEST_TOPOLOGY_ID));
        ipv4TopologyProvider.onTopologyBuilderRemoved(topology);
        result = (Map<TopologyId, TopologyReferenceSingletonService>) field.get(ipv4TopologyProvider);
        assertFalse(result.containsKey(this.TEST_TOPOLOGY_ID));
    }

}
