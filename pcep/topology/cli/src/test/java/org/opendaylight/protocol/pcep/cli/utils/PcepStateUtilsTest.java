/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.cli.utils;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.error.messages.grouping.ErrorMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.PeerPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PcepStateUtilsTest extends AbstractConcurrentDataBrokerTest {
    private static final String PCEP_TOPOLOGY = "pcep-topology";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String RIB_NOT_FOUND = "Node [pcc://" + IP_ADDRESS + "] not found\n";
    private static final String NODE_ID = "pcc://127.0.0.1";
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(output);

    @Test
    public void testNodeStateNotFound() {
        PcepStateUtils.displayNodeState(getDataBroker(), stream, PCEP_TOPOLOGY, NODE_ID);
        assertEquals(RIB_NOT_FOUND, output.toString());
    }

    @Test
    public void testDisplayNodeState() throws IOException, ExecutionException, InterruptedException {
        createDefaultProtocol();
        PcepStateUtils.displayNodeState(getDataBroker(), stream, PCEP_TOPOLOGY, NODE_ID);
        final String expected = Resources.toString(getClass().getClassLoader().getResource("node.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }

    private void createDefaultProtocol() throws ExecutionException, InterruptedException {
        final var tx = getDataBroker().newWriteOnlyTransaction();
        final var node = new NodeBuilder()
                .setNodeId(new NodeId(NODE_ID))
                .addAugmentation(new PcepTopologyNodeStatsAugBuilder().setPcepSessionState(createPcepSessionState())
                    .build())
                .build();

        tx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(PCEP_TOPOLOGY)))
            .child(Node.class, new NodeKey(new NodeId(NODE_ID)))
            .build(), node);
        tx.commit().get();
    }

    private static PcepSessionState createPcepSessionState() {
        final LocalPref pref = new LocalPrefBuilder()
                .setKeepalive(Uint8.valueOf(30))
                .setDeadtimer(Uint8.valueOf(120))
                .setIpAddress(IP_ADDRESS)
                .setSessionId(Uint16.ZERO)
                .setSpeakerEntityIdValue(SPEAKER_ID)
                .build();

        return new PcepSessionStateBuilder()
                .setSynchronized(Boolean.TRUE)
                .setSessionDuration("0:00:01:26")
                .setDelegatedLspsCount(Uint16.ONE)
                .setLocalPref(pref)
                .setPeerPref(new PeerPrefBuilder(pref).build())
                .setPeerCapabilities(new PeerCapabilitiesBuilder()
                    .setStateful(Boolean.TRUE)
                    .setInstantiation(Boolean.TRUE)
                    .setActive(Boolean.TRUE)
                    .build())
                .setMessages(new MessagesBuilder()
                    .setLastSentMsgTimestamp(Uint32.valueOf(1512043828L))
                    .setUnknownMsgReceived(Uint16.ONE)
                    .setSentMsgCount(Uint32.valueOf(5))
                    .setReceivedMsgCount(Uint32.valueOf(4))
                    .setReplyTime(new ReplyTimeBuilder()
                        .setAverageTime(Uint32.ONE)
                        .setMaxTime(Uint32.valueOf(3))
                        .setMinTime(Uint32.TWO)
                        .build())
                    .setErrorMessages(new ErrorMessagesBuilder()
                        .setReceivedErrorMsgCount(Uint32.ONE)
                        .setSentErrorMsgCount(Uint32.TWO)
                        .build())
                    .setLastReceivedRptMsgTimestamp(Uint32.valueOf(1512043769L))
                    .setSentUpdMsgCount(Uint32.ONE)
                    .setReceivedRptMsgCount(Uint32.TWO)
                    .setSentInitMsgCount(Uint32.valueOf(3))
                    .build())
                .build();
    }
}
