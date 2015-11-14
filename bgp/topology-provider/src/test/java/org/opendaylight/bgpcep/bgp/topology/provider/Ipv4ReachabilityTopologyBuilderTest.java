/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4ReachabilityTopologyBuilderTest extends AbstractTopologyBuilderTest {

    private static final String ROUTE_IP4PREFIX = "127.1.0.0/32";
    private static final String NEXT_HOP = "127.1.0.1";
    private static final String NEW_NEXT_HOP = "127.1.0.2";

    private Ipv4ReachabilityTopologyBuilder ipv4TopoBuilder;
    private InstanceIdentifier<Ipv4Route> ipv4RouteIID;

    @Override
    protected void setupWithDataBroker(final DataBroker dataBroker) {
        super.setupWithDataBroker(dataBroker);
        this.ipv4TopoBuilder = new Ipv4ReachabilityTopologyBuilder(dataBroker, LOC_RIB_REF, TEST_TOPOLOGY_ID);
        this.ipv4TopoBuilder.start(dataBroker, Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final InstanceIdentifier<Tables> path = this.ipv4TopoBuilder.tableInstanceIdentifier(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        this.ipv4RouteIID = path.builder().child((Class)Ipv4Routes.class).child(Ipv4Route.class, new Ipv4RouteKey(new Ipv4Prefix(ROUTE_IP4PREFIX))).build();
    }

    @Test
    public void testIpv4ReachabilityTopologyBuilder() throws TransactionCommitFailedException {
        // create route
        updateIpv4Route(createIpv4Route(NEXT_HOP));
        final Optional<Topology> topologyMaybe = getTopology(this.ipv4TopoBuilder.getInstanceIdentifier());
        assertTrue(topologyMaybe.isPresent());
        final Topology topology = topologyMaybe.get();
        assertEquals(1, topology.getNode().size());
        final Node node = topology.getNode().get(0);
        assertEquals(NEXT_HOP, node.getNodeId().getValue());
        assertEquals(ROUTE_IP4PREFIX, node.getAugmentation(Node1.class).getIgpNodeAttributes().getPrefix().get(0).getPrefix().getIpv4Prefix().getValue());

        // update route
        updateIpv4Route(createIpv4Route(NEW_NEXT_HOP));
        final Topology topologyUpdated = getTopology(this.ipv4TopoBuilder.getInstanceIdentifier()).get();
        assertEquals(1, topologyUpdated.getNode().size());
        final Node nodeUpdated = topologyUpdated.getNode().get(0);
        assertEquals(NEW_NEXT_HOP, nodeUpdated.getNodeId().getValue());
        assertEquals(ROUTE_IP4PREFIX, nodeUpdated.getAugmentation(Node1.class).getIgpNodeAttributes().getPrefix().get(0).getPrefix().getIpv4Prefix().getValue());

        // delete route
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID);
        wTx.submit();
        final Topology topologyDeleted = getTopology(this.ipv4TopoBuilder.getInstanceIdentifier()).get();
        assertEquals(0, topologyDeleted.getNode().size());

        this.ipv4TopoBuilder.close();
        assertFalse(getTopology(this.ipv4TopoBuilder.getInstanceIdentifier()).isPresent());
    }

    private void updateIpv4Route(final Ipv4Route data) {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, this.ipv4RouteIID, data, true);
        wTx.submit();
    }

    private Ipv4Route createIpv4Route(final String nextHop) {
        final Attributes attribute = new AttributesBuilder()
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address(nextHop)).build()).build())
            .build();
        return new Ipv4RouteBuilder().setKey(new Ipv4RouteKey(new Ipv4Prefix(ROUTE_IP4PREFIX))).setPrefix(new Ipv4Prefix(ROUTE_IP4PREFIX)).setAttributes(attribute).build();
    }

}
