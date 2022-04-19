/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesRpcAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class TopologyStatsRpcServiceImplTest extends AbstractConcurrentDataBrokerTest {
    private static final String TOPOLOGY_ID1 = "pcep-topology-1";
    private static final String TOPOLOGY_ID2 = "pcep-topology-2";
    private static final String NONEXISTENT_TOPOLOGY = "nonexistent-topology";
    private static final String NONPCEP_TOPOLOGY = "nonpcep-topology";
    private static final String NODE_ID1 = "pcc://1.1.1.1";
    private static final String NODE_ID2 = "pcc://2.2.2.2";
    private static final String NODE_ID3 = "pcc://3.3.3.3";
    private static final String NONEXISTENT_NODE = "pcc://4.4.4.4";
    private static final String NONPCEP_NODE = "nonpcep-node";

    TopologyStatsRpcServiceImpl rpcService;

    @Before
    public void setUp() throws Exception {
        rpcService = new TopologyStatsRpcServiceImpl(getDataBroker());

        // PCEP topology with one PCC node
        final Topology t1 = createTopology(TOPOLOGY_ID1, BindingMap.of(createPcepNode(NODE_ID1)));

        // PCEP topology with two PCC node
        final Topology t2 =
                createTopology(TOPOLOGY_ID2, BindingMap.of(createPcepNode(NODE_ID2), createPcepNode(NODE_ID3)));

        // Non-PCEP topology with one non-PCC node
        final Topology t3 = createTopology(NONPCEP_TOPOLOGY,
                BindingMap.of(new NodeBuilder().setNodeId(new NodeId(NONPCEP_NODE)).build()));

        final WriteTransaction wtx = getDataBroker().newWriteOnlyTransaction();
        final NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
        ntb.setTopology(BindingMap.of(t1, t2, t3));
        wtx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(NetworkTopology.class).build(),
                ntb.build());
        wtx.commit().get();
    }

    private static Topology createTopology(final String topologyId, final Map<NodeKey, Node> nodes) {
        return new TopologyBuilder().setTopologyId(new TopologyId(topologyId)).setNode(nodes).build();
    }

    private static Node createPcepNode(final String nodeId) {
        return new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .addAugmentation(new PcepTopologyNodeStatsAugBuilder()
                    .setPcepSessionState(createTopologySessionState())
                    .build())
                .build();
    }

    private static PcepSessionState createTopologySessionState() {
        final ReplyTime replyTime = new ReplyTimeBuilder()
                .setAverageTime(Uint32.ONE)
                .setMaxTime(Uint32.valueOf(3))
                .setMinTime(Uint32.TWO)
                .build();

        final ErrorMessages errorMsg = new ErrorMessagesBuilder()
                .setReceivedErrorMsgCount(Uint32.ONE)
                .setSentErrorMsgCount(Uint32.valueOf(2))
                .build();

        final StatefulMessagesStatsAug statefulMsg = new StatefulMessagesStatsAugBuilder()
                .setLastReceivedRptMsgTimestamp(Uint32.valueOf(1553183614L))
                .setSentUpdMsgCount(Uint32.ONE)
                .setReceivedRptMsgCount(Uint32.TWO)
                .setSentInitMsgCount(Uint32.valueOf(3))
                .build();

        final Messages messages = new MessagesBuilder()
                .setLastSentMsgTimestamp(Uint32.valueOf(1553183734L))
                .setUnknownMsgReceived(Uint16.ONE)
                .setSentMsgCount(Uint32.valueOf(5))
                .setReceivedMsgCount(Uint32.valueOf(4))
                .setReplyTime(replyTime)
                .setErrorMessages(errorMsg)
                .addAugmentation(statefulMsg).build();

        final PeerCapabilities capabilities = new PeerCapabilitiesBuilder()
                .addAugmentation(new StatefulCapabilitiesStatsAugBuilder()
                    .setStateful(true)
                    .setInstantiation(true)
                    .setActive(true)
                    .build())
                .build();

        final LocalPref localPref = new LocalPrefBuilder()
                .setKeepalive(Uint8.valueOf(30))
                .setDeadtimer(Uint8.valueOf(120))
                .setIpAddress("127.0.0.1")
                .setSessionId(Uint16.ZERO)
                .addAugmentation(new PcepEntityIdStatsAugBuilder()
                    .setSpeakerEntityIdValue(new byte[] {0x01, 0x02, 0x03, 0x04})
                    .build())
                .build();

        return new PcepSessionStateBuilder().setSynchronized(true).setSessionDuration("0:00:05:18")
                .setDelegatedLspsCount(Uint16.ONE).setLocalPref(localPref)
                .setPeerPref(new PeerPrefBuilder(localPref).build())
                .setPeerCapabilities(capabilities).setMessages(messages).build();
    }

    private static PcepSessionState createRpcSessionState() {
        final ReplyTime replyTime = new ReplyTimeBuilder()
                .setAverageTime(Uint32.ONE)
                .setMaxTime(Uint32.valueOf(3))
                .setMinTime(Uint32.TWO)
                .build();

        final ErrorMessages errorMsg = new ErrorMessagesBuilder()
                .setReceivedErrorMsgCount(Uint32.ONE).setSentErrorMsgCount(Uint32.TWO).build();

        final StatefulMessagesRpcAug statefulMsg = new StatefulMessagesRpcAugBuilder()
                .setLastReceivedRptMsgTimestamp(Uint32.valueOf(1553183614L))
                .setSentUpdMsgCount(Uint32.ONE)
                .setReceivedRptMsgCount(Uint32.TWO)
                .setSentInitMsgCount(Uint32.valueOf(3))
                .build();

        final Messages messages = new MessagesBuilder()
                .setLastSentMsgTimestamp(Uint32.valueOf(1553183734L))
                .setUnknownMsgReceived(Uint16.ONE)
                .setSentMsgCount(Uint32.valueOf(5))
                .setReceivedMsgCount(Uint32.valueOf(4))
                .setReplyTime(replyTime)
                .setErrorMessages(errorMsg)
                .addAugmentation(statefulMsg).build();

        final PeerCapabilities capabilities = new PeerCapabilitiesBuilder()
                .addAugmentation(new StatefulCapabilitiesRpcAugBuilder()
                        .setStateful(true).setInstantiation(true).setActive(true).build())
                .build();

        final LocalPref localPref = new LocalPrefBuilder()
                .setKeepalive(Uint8.valueOf(30))
                .setDeadtimer(Uint8.valueOf(120))
                .setIpAddress("127.0.0.1")
                .setSessionId(Uint16.ZERO)
                .addAugmentation(new PcepEntityIdRpcAugBuilder()
                    .setSpeakerEntityIdValue(new byte[] {0x01, 0x02, 0x03, 0x04})
                    .build())
                .build();

        return new PcepSessionStateBuilder().setSynchronized(true).setSessionDuration("0:00:05:18")
                .setDelegatedLspsCount(Uint16.ONE).setLocalPref(localPref)
                .setPeerPref(new PeerPrefBuilder(localPref).build())
                .setPeerCapabilities(capabilities).setMessages(messages)
                .build();
    }

    @Test
    public void testGetStatsNoMatch() throws Exception {
        GetStatsInput in;
        GetStatsOutput out;

        // Non-existing topology
        in = createGetStatsInput(NONEXISTENT_TOPOLOGY, null);
        out = createGetStatsOutput(NONEXISTENT_TOPOLOGY, Collections.emptyList(), null);
        performTest(in, out);

        // Non-existent node
        in = createGetStatsInput(TOPOLOGY_ID1, Collections.singletonList(NONEXISTENT_NODE));
        out = createGetStatsOutput(TOPOLOGY_ID1, Collections.singletonList(NONEXISTENT_NODE), null);
        performTest(in, out);

        // Non-PCEP topology
        in = createGetStatsInput(NONPCEP_TOPOLOGY, Collections.singletonList(NONPCEP_NODE));
        out = createGetStatsOutput(NONPCEP_TOPOLOGY, Collections.singletonList(NONPCEP_NODE), null);
        performTest(in, out);
    }

    @Test
    public void testGetStatsPartialMatch() throws Exception {
        GetStatsInput in;
        GetStatsOutput out;

        // Match one PCEP topology
        in = createGetStatsInput(TOPOLOGY_ID1, null);
        out = createGetStatsOutput(TOPOLOGY_ID1, Collections.singletonList(NODE_ID1), createRpcSessionState());
        performTest(in, out);

        // Match one PCEP node in one topology
        in = createGetStatsInput(TOPOLOGY_ID2, Collections.singletonList(NODE_ID3));
        out = createGetStatsOutput(TOPOLOGY_ID2, Collections.singletonList(NODE_ID3), createRpcSessionState());
        performTest(in, out);

        // Match two PCEP nodes in one topology
        in = createGetStatsInput(TOPOLOGY_ID2, Arrays.asList(NODE_ID2, NODE_ID3));
        out = createGetStatsOutput(TOPOLOGY_ID2, Arrays.asList(NODE_ID2, NODE_ID3), createRpcSessionState());
        performCountTest(in, out);
    }

    @Test
    public void testGetStatsAllMatch() throws Exception {
        GetStatsInput in;

        final var ot1 = createGetStatsOutput(TOPOLOGY_ID1, Collections.singletonList(NODE_ID1), createRpcSessionState())
                .getTopology().values() .iterator().next();
        final var ot2 = createGetStatsOutput(TOPOLOGY_ID2, Arrays.asList(NODE_ID2, NODE_ID3), createRpcSessionState())
                .getTopology().values().iterator().next();
        final GetStatsOutput out = new GetStatsOutputBuilder().setTopology(BindingMap.of(ot1, ot2)).build();

        // Implicitly match all PCEP topologies and nodes
        in = createGetStatsInput(null, null);
        performCountTest(in, out);

        // Explicitly match all PCEP topologies and nodes
        final var it1 = createGetStatsInput(TOPOLOGY_ID1, Collections.singletonList(NODE_ID1)).getTopology().values()
                .iterator().next();
        final var it2 = createGetStatsInput(TOPOLOGY_ID2, Arrays.asList(NODE_ID2, NODE_ID3)).getTopology().values()
                .iterator().next();
        in = new GetStatsInputBuilder().setTopology(BindingMap.of(it1, it2)).build();
        performCountTest(in, out);
    }

    private void performTest(final GetStatsInput in, final GetStatsOutput out) throws Exception {
        final RpcResult<GetStatsOutput> result = rpcService.getStats(in).get();
        assertEquals(out, result.getResult());
        assertTrue(result.isSuccessful());
        assertTrue(result.getErrors().isEmpty());
    }

    /*
     * When topology and/or node list is expected to contain more than one item,
     * direct comparison will fail due to potential list ordering differences. So
     * just compare the number of nodes
     */
    private void performCountTest(final GetStatsInput in, final GetStatsOutput out) throws Exception {
        final RpcResult<GetStatsOutput> result = rpcService.getStats(in).get();
        assertEquals(result.getResult().getTopology().size(), out.getTopology().size());
        assertTrue(result.isSuccessful());
        assertEquals(result.getResult().nonnullTopology().values().stream()
            .flatMap(t -> t.nonnullNode().values().stream()).count(),
            out.nonnullTopology().values().stream().flatMap(t -> t.nonnullNode().values().stream()).count());
        assertTrue(result.isSuccessful());
        assertTrue(result.getErrors().isEmpty());
    }

    private static GetStatsInput createGetStatsInput(final String topologyId, final List<String> nodeIds) {
        final Map<
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321
                .get.stats.input.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321
                .get.stats.input.topology.Node> nodes;
        if (nodeIds != null) {
            nodes = nodeIds.stream()
                    .map(nodeId -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology
                        .stats.rpc.rev190321.get.stats.input.topology.NodeBuilder()
                            .setNodeId(new NodeId(nodeId))
                            .build())
                .collect(BindingMap.toOrderedMap());
        } else {
            nodes = null;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get
            .stats.input.Topology topology;
        if (topologyId != null) {
            topology = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc
                    .rev190321.get.stats.input.TopologyBuilder()
                        .setTopologyId(new TopologyId(topologyId))
                        .setNode(nodes)
                        .build();
        } else {
            topology = null;
        }
        return new GetStatsInputBuilder().setTopology(topology != null ? BindingMap.of(topology) : null).build();
    }

    private static GetStatsOutput createGetStatsOutput(final String topologyId, final List<String> nodeIds,
            final PcepSessionState state) {
        final Map<
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321
                .get.stats.output.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321
                    .get.stats.output.topology.Node> nodes;
        if (nodeIds != null) {
            nodes = nodeIds.stream()
                    .map(nodeId -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology
                        .stats.rpc.rev190321.get.stats.output.topology.NodeBuilder()
                            .setNodeId(new NodeId(nodeId))
                            .setPcepSessionState(state)
                            .build())
                .collect(BindingMap.toOrderedMap());
        } else {
            nodes = null;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get
            .stats.output.Topology topology;
        if (topologyId != null) {
            topology = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc
                    .rev190321.get.stats.output.TopologyBuilder()
                            .setTopologyId(new TopologyId(topologyId))
                            .setNode(nodes)
                            .build();
        } else {
            topology = null;
        }
        return new GetStatsOutputBuilder().setTopology(topology != null ? BindingMap.of(topology) : null).build();
    }

    @After
    public void tearDown() {
        rpcService.close();
    }
}
