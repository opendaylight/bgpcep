/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.cli.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulCapabilitiesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulMessagesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulMessagesStatsAugBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PcepStateUtilsTest extends AbstractConcurrentDataBrokerTest {
    private static final String PCEP_TOPOLOGY = "pcep-topology";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String RIB_NOT_FOUND = "Node [pcc://" + IP_ADDRESS + "] not found\n";
    private static final String NODE_ID = "pcc://127.0.0.1";
    private static final String UTF8 = "UTF-8";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    @Test
    public void testNodeStateNotFound() {
        PcepStateUtils.displayNodeState(getDataBroker(), this.stream, PCEP_TOPOLOGY, NODE_ID);
        assertEquals(RIB_NOT_FOUND, this.output.toString());
    }

    @Test
    public void testDisplayNodeState() throws IOException, ExecutionException, InterruptedException {
        createDefaultProtocol();
        PcepStateUtils.displayNodeState(getDataBroker(), this.stream, PCEP_TOPOLOGY, NODE_ID);
        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("node.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    private void createDefaultProtocol() throws ExecutionException, InterruptedException {
        final WriteTransaction wt = getDataBroker().newWriteOnlyTransaction();
        final Node node = new NodeBuilder()
                .setNodeId(new NodeId(NODE_ID))
                .addAugmentation(PcepTopologyNodeStatsAug.class,
                        new PcepTopologyNodeStatsAugBuilder().setPcepSessionState(createPcepSessionState())
                                .build())
                .build();

        final InstanceIdentifier<Node> topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(PCEP_TOPOLOGY)))
                .child(Node.class, new NodeKey(new NodeId(NODE_ID))).build();
        wt.put(LogicalDatastoreType.OPERATIONAL, topology, node, true);
        wt.submit().get();
    }

    private PcepSessionState createPcepSessionState() {
        final LocalPref pref = new LocalPrefBuilder()
                .setKeepalive((short) 30)
                .setDeadtimer((short) 120)
                .setIpAddress(IP_ADDRESS)
                .setSessionId(0)
                .build();

        final PeerCapabilities capa = new PeerCapabilitiesBuilder()
                .addAugmentation(StatefulCapabilitiesStatsAug.class, new StatefulCapabilitiesStatsAugBuilder()
                        .setStateful(true)
                        .setInstantiation(true)
                        .setActive(true)
                        .build())
                .build();

        final ReplyTime reply = new ReplyTimeBuilder()
                .setAverageTime(1L)
                .setMaxTime(3L)
                .setMinTime(2L)
                .build();

        final ErrorMessages errorMsg = new ErrorMessagesBuilder()
                .setReceivedErrorMsgCount(1L)
                .setSentErrorMsgCount(2L)
                .build();

        final StatefulMessagesStatsAug statefulMsg = new StatefulMessagesStatsAugBuilder()
                .setLastReceivedRptMsgTimestamp(1512043769L)
                .setSentUpdMsgCount(1L)
                .setReceivedRptMsgCount(2L)
                .setSentInitMsgCount(3L)
                .build();

        final Messages messages = new MessagesBuilder()
                .setLastSentMsgTimestamp(1512043828L)
                .setUnknownMsgReceived(1)
                .setSentMsgCount(5L)
                .setReceivedMsgCount(4L)
                .setReplyTime(reply)
                .setErrorMessages(errorMsg)
                .addAugmentation(StatefulMessagesStatsAug.class, statefulMsg)
                .build();

        return new PcepSessionStateBuilder()
                .setSynchronized(true)
                .setSessionDuration("0:00:01:26")
                .setDelegatedLspsCount(1)
                .setLocalPref(pref)
                .setPeerPref(new PeerPrefBuilder(pref).build())
                .setPeerCapabilities(capa)
                .setMessages(messages)
                .build();
    }
}