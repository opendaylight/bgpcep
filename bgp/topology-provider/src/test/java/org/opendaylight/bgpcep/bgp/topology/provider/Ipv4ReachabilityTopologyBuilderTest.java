/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4ReachabilityTopologyBuilderTest extends AbstractTopologyBuilderTest {

    static final long PATH_ID = 1;
    private static final String ROUTE_IP4PREFIX = "127.1.0.0/32";
    private static final String NEXT_HOP = "127.1.0.1";
    private static final String NEW_NEXT_HOP = "127.1.0.2";

    private Ipv4ReachabilityTopologyBuilder ipv4TopoBuilder;
    private InstanceIdentifier<Ipv4Route> ipv4RouteIID;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        this.ipv4TopoBuilder = new Ipv4ReachabilityTopologyBuilder(getDataBroker(), LOC_RIB_REF, TEST_TOPOLOGY_ID);
        this.ipv4TopoBuilder.start();
        final InstanceIdentifier<Tables> path = LOC_RIB_REF.getInstanceIdentifier().builder().child(LocRib.class)
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)).build();

        this.ipv4RouteIID = path.builder().child((Class) Ipv4Routes.class)
            .child(Ipv4Route.class, new Ipv4RouteKey(new PathId(PATH_ID), new Ipv4Prefix(ROUTE_IP4PREFIX))).build();
    }

    @Test
    public void testIpv4ReachabilityTopologyBuilder() throws TransactionCommitFailedException, ReadFailedException {
        // create route
        updateIpv4Route(createIpv4Route(NEXT_HOP));

        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            final TopologyTypes1 topologyTypes = topology.getTopologyTypes().getAugmentation(TopologyTypes1.class);
            assertNotNull(topologyTypes);
            assertNotNull(topologyTypes.getBgpIpv4ReachabilityTopology());
            assertEquals(1, topology.getNode().size());
            final Node node = topology.getNode().get(0);
            assertEquals(NEXT_HOP, node.getNodeId().getValue());
            assertEquals(ROUTE_IP4PREFIX, node.getAugmentation(Node1.class).getIgpNodeAttributes().getPrefix().get(0)
                .getPrefix().getIpv4Prefix().getValue());
            return topology;
        });

        // update route
        updateIpv4Route(createIpv4Route(NEW_NEXT_HOP));
        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.getNode().size());
            final Node nodeUpdated = topology.getNode().get(0);
            assertEquals(NEW_NEXT_HOP, nodeUpdated.getNodeId().getValue());
            assertEquals(ROUTE_IP4PREFIX, nodeUpdated.getAugmentation(Node1.class).getIgpNodeAttributes()
                .getPrefix().get(0).getPrefix().getIpv4Prefix().getValue());
            return topology;
        });

        // delete route
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID);
        wTx.submit();
        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(0, topology.getNode().size());
            return topology;
        });

        this.ipv4TopoBuilder.close();
        checkNotPresentOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier());
    }

    private void updateIpv4Route(final Ipv4Route data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID, data, true);
        wTx.submit();
    }

    private static Ipv4Route createIpv4Route(final String nextHop) {
        final Attributes attribute = new AttributesBuilder()
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address(nextHop)).build()).build()).build();
        return new Ipv4RouteBuilder().setKey(new Ipv4RouteKey(new PathId(PATH_ID), new Ipv4Prefix(ROUTE_IP4PREFIX)))
            .setPrefix(new Ipv4Prefix(ROUTE_IP4PREFIX)).setAttributes(attribute).build();
    }

}
