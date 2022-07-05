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
import static org.junit.Assert.assertNull;
import static org.opendaylight.bgpcep.bgp.topology.provider.Ipv4ReachabilityTopologyBuilderTest.PATH_ID;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6ReachabilityTopologyBuilderTest extends AbstractTopologyBuilderTest {

    private static final String ROUTE_IP6PREFIX = "2001:db8:a::123/64";
    private static final String NEXT_HOP = "2001:db8:a::124";
    private static final String NEW_NEXT_HOP = "2001:db8:a::125";

    private Ipv6ReachabilityTopologyBuilder ipv6TopoBuilder;
    private InstanceIdentifier<Ipv6Route> ipv6RouteIID;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        this.ipv6TopoBuilder = new Ipv6ReachabilityTopologyBuilder(getDataBroker(), LOC_RIB_REF, TEST_TOPOLOGY_ID);
        this.ipv6TopoBuilder.start();
        final InstanceIdentifier<Tables> path = LOC_RIB_REF.getInstanceIdentifier().builder().child(LocRib.class)
            .child(Tables.class, new TablesKey(Ipv6AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)).build();
        this.ipv6RouteIID = path.builder().child(Ipv6RoutesCase.class, Ipv6Routes.class)
            .child(Ipv6Route.class, new Ipv6RouteKey(new PathId(PATH_ID), ROUTE_IP6PREFIX)).build();
    }

    @Test
    public void testIpv6ReachabilityTopologyBuilder() throws InterruptedException, ExecutionException {
        // create route
        updateIpv6Route(createIpv6Route(NEXT_HOP));

        readDataOperational(getDataBroker(), this.ipv6TopoBuilder.getInstanceIdentifier(), topology -> {
            final TopologyTypes1 topologyType = topology.getTopologyTypes().augmentation(TopologyTypes1.class);
            assertNotNull(topologyType);
            assertNotNull(topologyType.getBgpIpv6ReachabilityTopology());
            assertEquals(1, topology.nonnullNode().size());
            final Node node = topology.nonnullNode().values().iterator().next();
            assertEquals(NEXT_HOP, node.getNodeId().getValue());
            assertEquals(ROUTE_IP6PREFIX, node.augmentation(Node1.class).getIgpNodeAttributes().nonnullPrefix()
                .values().iterator().next().getPrefix().getIpv6Prefix().getValue());
            return topology;
        });

        // update route
        updateIpv6Route(createIpv6Route(NEW_NEXT_HOP));

        readDataOperational(getDataBroker(), this.ipv6TopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullNode().size());
            final Node nodeUpdated = topology.nonnullNode().values().iterator().next();
            assertEquals(NEW_NEXT_HOP, nodeUpdated.getNodeId().getValue());
            assertEquals(ROUTE_IP6PREFIX, nodeUpdated.augmentation(Node1.class).getIgpNodeAttributes().nonnullPrefix()
                .values().iterator().next().getPrefix().getIpv6Prefix().getValue());
            return topology;
        });

        // delete route
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.ipv6RouteIID);
        wTx.commit();
        readDataOperational(getDataBroker(), this.ipv6TopoBuilder.getInstanceIdentifier(), topology -> {
            assertNull(topology.getNode());
            return topology;
        });

        this.ipv6TopoBuilder.close();
        checkNotPresentOperational(getDataBroker(), this.ipv6TopoBuilder.getInstanceIdentifier());
    }

    private void updateIpv6Route(final Ipv6Route data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, this.ipv6RouteIID, data);
        wTx.commit();
    }

    private static Ipv6Route createIpv6Route(final String nextHop) {
        final Attributes attribute = new AttributesBuilder()
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                new Ipv6NextHopBuilder().setGlobal(new Ipv6AddressNoZone(nextHop)).build()).build()).build();
        return new Ipv6RouteBuilder().withKey(new Ipv6RouteKey(new PathId(PATH_ID), ROUTE_IP6PREFIX))
            .setPrefix(new Ipv6Prefix(ROUTE_IP6PREFIX)).setAttributes(attribute).build();
    }
}
