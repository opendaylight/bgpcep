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

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.Ipv4ReachabilityTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1Builder;

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

}
