/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Path1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.path.computation.client.ReportedLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.path.computation.client.reported.lsp.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.path.computation.client.reported.lsp.PathKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;

public class NodeChangedListenerTest extends AbstractConcurrentDataBrokerTest {

    private static final TopologyId PCEP_TOPOLOGY_ID = new TopologyId("pcep-topology");
    private static final TopologyId TUNNEL_TOPOLOGY_ID = new TopologyId("tunnel-topology");

    private static final String NODE1_IPV4 = "39.39.39.39";
    private static final NodeId NODE1_ID = new NodeId("pcc://" + NODE1_IPV4);
    private static final String LSP1_NAME = "lsp1";
    private static final Uint32 LSP1_ID = Uint32.ONE;

    private static final String NODE2_IPV4 = "40.40.40.40";
    private static final NodeId NODE2_ID = new NodeId("pcc://" + NODE2_IPV4);
    private static final String LSP2_NAME = "lsp2";
    private static final Uint32 LSP2_ID = Uint32.TWO;

    private static final InstanceIdentifier<Topology> PCEP_TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(PCEP_TOPOLOGY_ID)).build();
    private static final InstanceIdentifier<Topology> TUNNEL_TOPO_IID = InstanceIdentifier
            .builder(NetworkTopology.class).child(Topology.class, new TopologyKey(TUNNEL_TOPOLOGY_ID)).build();

    private ListenerRegistration<NodeChangedListener> listenerRegistration;

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, PCEP_TOPO_IID, new TopologyBuilder()
                .withKey(new TopologyKey(PCEP_TOPOLOGY_ID))
                .setTopologyId(PCEP_TOPOLOGY_ID).build());
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, TUNNEL_TOPO_IID, new TopologyBuilder()
                .withKey(new TopologyKey(TUNNEL_TOPOLOGY_ID)).setTopologyId(TUNNEL_TOPOLOGY_ID).build());
        wTx.commit().get();
        final NodeChangedListener nodeListener = new NodeChangedListener(getDataBroker(),
                PCEP_TOPOLOGY_ID, TUNNEL_TOPO_IID);
        this.listenerRegistration = getDataBroker().registerDataTreeChangeListener(DataTreeIdentifier.create(
                LogicalDatastoreType.OPERATIONAL, PCEP_TOPO_IID.child(Node.class)), nodeListener);
    }

    @Test
    public void testNodeChangedListener() throws InterruptedException, ExecutionException {
        // add node -> create two nodes with TPs and link
        createNode(NODE1_ID, NODE1_IPV4, LSP1_NAME, LSP1_ID, NODE2_IPV4);
        final Topology tunnelTopo = readDataOperational(getDataBroker(), TUNNEL_TOPO_IID, tunnelTopo1 -> {
            assertNotNull(tunnelTopo1.getNode());
            assertEquals(2, tunnelTopo1.getNode().size());
            return tunnelTopo1;
        });

        final NodeId srcId = new NodeId("ip://" + new IpAddress(new Ipv4Address(NODE1_IPV4)));
        final NodeId dstId = new NodeId("ip://" + new IpAddress(new Ipv4Address(NODE2_IPV4)));

        final Node dst;
        final Node src;

        final Iterator<Node> it = tunnelTopo.nonnullNode().values().iterator();
        final Node tmp = it.next();
        if (tmp.getNodeId().equals(srcId)) {
            src = tmp;
            dst = it.next();
        } else {
            src = it.next();
            dst = tmp;
        }

        assertEquals(srcId, src.getNodeId());
        assertEquals(dstId, dst.getNodeId());

        assertEquals(1, dst.getTerminationPoint().size());
        assertEquals(1, src.getTerminationPoint().size());
        final TerminationPoint dstTp = dst.nonnullTerminationPoint().values().iterator().next();
        final TerminationPoint srcTp = src.nonnullTerminationPoint().values().iterator().next();
        final TpId dstNodeTpId = new TpId(dstId.getValue());
        final TpId srcNodeTpId = new TpId(srcId.getValue());
        assertEquals(dstNodeTpId, dstTp.getTpId());
        assertEquals(srcNodeTpId, srcTp.getTpId());

        assertEquals(1, src.getSupportingNode().size());
        assertNull(dst.getSupportingNode());
        final SupportingNode sNode = src.nonnullSupportingNode().values().iterator().next();
        assertEquals(NODE1_ID, sNode.key().getNodeRef());

        assertEquals(1, tunnelTopo.nonnullLink().size());
        final Link link = tunnelTopo.nonnullLink().values().iterator().next();
        assertEquals(srcId, link.getSource().getSourceNode());
        assertEquals(srcNodeTpId, link.getSource().getSourceTp());
        assertEquals(dstId, link.getDestination().getDestNode());
        assertEquals(dstNodeTpId, link.getDestination().getDestTp());

        // update second node -> adds supporting node and second link
        createNode(NODE2_ID, NODE2_IPV4, LSP2_NAME, LSP2_ID, NODE1_IPV4);
        readDataOperational(getDataBroker(), TUNNEL_TOPO_IID, updatedNodeTopo -> {
            assertNotNull(updatedNodeTopo.getNode());
            assertEquals(2, updatedNodeTopo.getNode().size());
            final Node updatedNode;
            if (updatedNodeTopo.nonnullNode().values().iterator().next().getNodeId().equals(srcId)) {
                updatedNode = Iterables.get(updatedNodeTopo.nonnullNode().values(), 1);
            } else {
                updatedNode = updatedNodeTopo.nonnullNode().values().iterator().next();
            }

            assertNotNull(updatedNode.getSupportingNode());
            assertEquals(1, updatedNode.nonnullSupportingNode().size());
            final SupportingNode sNode2 = updatedNode.nonnullSupportingNode().values().iterator().next();
            assertEquals(NODE2_ID, sNode2.getNodeRef());
            assertEquals(2, updatedNodeTopo.getLink().size());
            return updatedNodeTopo;

        });

        readDataOperational(getDataBroker(), TUNNEL_TOPO_IID, updatedNodeTopo -> {
            Link link2;
            Iterator<Link> it2 = updatedNodeTopo.nonnullLink().values().iterator();
            link2 = it2.next();
            if (srcId.equals(link2.getSource().getSourceNode())) {
                link2 = it2.next();
            }

            assertEquals(dstId, link2.getSource().getSourceNode());
            assertEquals(dstNodeTpId, link2.getSource().getSourceTp());
            assertEquals(srcId, link2.getDestination().getDestNode());
            assertEquals(srcNodeTpId, link2.getDestination().getDestTp());
            return updatedNodeTopo;
        });

        // remove nodes -> remove link
        removeNode(NODE1_ID);
        removeNode(NODE2_ID);
        readDataOperational(getDataBroker(), TUNNEL_TOPO_IID, removedNodeTopo -> {
            assertNull(removedNodeTopo.getNode());
            assertNull(removedNodeTopo.getLink());
            return removedNodeTopo;
        });
    }

    @After
    public void tearDown() {
        this.listenerRegistration.close();
    }

    private void createNode(final NodeId nodeId, final String ipv4Address, final String lspName, final Uint32 lspId,
            final String dstIpv4Address) throws InterruptedException, ExecutionException {
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.withKey(new NodeKey(nodeId));
        nodeBuilder.setNodeId(nodeId);
        final PathBuilder pathBuilder = new PathBuilder();
        pathBuilder.withKey(new PathKey(new LspId(lspId)));
        pathBuilder.setBandwidth(new BandwidthBuilder().setBandwidth(
                new Bandwidth(new byte[]{0x00, 0x00, (byte) 0xff, (byte) 0xff})).build());
        pathBuilder.addAugmentation(new Path1Builder().setLsp(new LspBuilder().setTlvs(new TlvsBuilder()
                .setLspIdentifiers(new LspIdentifiersBuilder().setAddressFamily(new Ipv4CaseBuilder().setIpv4(
                        new Ipv4Builder().setIpv4TunnelSenderAddress(new Ipv4AddressNoZone(ipv4Address))
                                .setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(ipv4Address))
                                .setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone(dstIpv4Address))
                                .build()).build()).build()).build()).setAdministrative(true)
                .setDelegate(true).build()).build());
        final ReportedLsp reportedLps = new ReportedLspBuilder().withKey(new ReportedLspKey(lspName)).setPath(
                BindingMap.of(pathBuilder.build())).build();
        final Node1Builder node1Builder = new Node1Builder();
        node1Builder.setPathComputationClient(new PathComputationClientBuilder()
                .setStateSync(PccSyncState.Synchronized)
                .setReportedLsp(BindingMap.of(reportedLps))
                .setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone(ipv4Address)))
                .build());
        nodeBuilder.addAugmentation(node1Builder.build());
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, PCEP_TOPO_IID.builder().child(Node.class,
                new NodeKey(nodeId)).build(), nodeBuilder.build());
        wTx.commit().get();
    }

    private void removeNode(final NodeId nodeId) throws InterruptedException, ExecutionException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, PCEP_TOPO_IID.builder()
                .child(Node.class, new NodeKey(nodeId)).build());
        wTx.commit().get();
    }
}
