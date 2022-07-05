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
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;

public class Ipv4ReachabilityTopologyBuilderTest extends AbstractTopologyBuilderTest {

    static final Uint32 PATH_ID = Uint32.ONE;
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
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)).build();

        this.ipv4RouteIID = path.builder().child(Ipv4RoutesCase.class, Ipv4Routes.class)
            .child(Ipv4Route.class, new Ipv4RouteKey(new PathId(PATH_ID), ROUTE_IP4PREFIX)).build();
    }

    @Test
    public void testIpv4ReachabilityTopologyBuilder() throws InterruptedException, ExecutionException {
        // create route
        updateIpv4Route(createIpv4Route(NEXT_HOP));

        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            final TopologyTypes1 topologyTypes = topology.getTopologyTypes().augmentation(TopologyTypes1.class);
            assertNotNull(topologyTypes);
            assertNotNull(topologyTypes.getBgpIpv4ReachabilityTopology());
            assertEquals(1, topology.nonnullNode().size());
            final Node node = topology.nonnullNode().values().iterator().next();
            assertEquals(NEXT_HOP, node.getNodeId().getValue());
            assertEquals(ROUTE_IP4PREFIX, node.augmentation(Node1.class).getIgpNodeAttributes().nonnullPrefix().values()
                .iterator().next().getPrefix().getIpv4Prefix().getValue());
            return topology;
        });

        // update route
        updateIpv4Route(createIpv4Route(NEW_NEXT_HOP));
        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            assertEquals(1, topology.nonnullNode().size());
            final Node nodeUpdated = topology.nonnullNode().values().iterator().next();
            assertEquals(NEW_NEXT_HOP, nodeUpdated.getNodeId().getValue());
            assertEquals(ROUTE_IP4PREFIX, nodeUpdated.augmentation(Node1.class).getIgpNodeAttributes()
                .nonnullPrefix().values().iterator().next().getPrefix().getIpv4Prefix().getValue());
            return topology;
        });

        // delete route
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID);
        wTx.commit();
        readDataOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier(), topology -> {
            assertNull(topology.getNode());
            return topology;
        });

        this.ipv4TopoBuilder.close();
        checkNotPresentOperational(getDataBroker(), this.ipv4TopoBuilder.getInstanceIdentifier());
    }

    private void updateIpv4Route(final Ipv4Route data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID, data);
        wTx.commit();
    }

    private static Ipv4Route createIpv4Route(final String nextHop) {
        final Attributes attribute = new AttributesBuilder()
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4AddressNoZone(nextHop)).build()).build()).build();
        return new Ipv4RouteBuilder().withKey(new Ipv4RouteKey(new PathId(PATH_ID), ROUTE_IP4PREFIX))
            .setPrefix(new Ipv4Prefix(ROUTE_IP4PREFIX)).setAttributes(attribute).build();
    }

}
