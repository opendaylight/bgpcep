/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.error.messages.LastReceivedErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.error.messages.LastSentErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class Bgpcep1042Test extends AbstractPCEPSessionTest {
    /**
     * Test of pcep stats update synchronization.
     *
     * <p>Test that {@link SessionStateUpdater#updateStatistics()} is synchronized properly. It is using transaction
     * chain of {@link TopologyNodeState}, so verify there is no race with node reads.
     */
    @Test
    public void testSessionStateUpdaterSynchronization() throws Exception {
        final var listener = getSessionListener();
        final var session = getPCEPSession(getLocalPref(), getRemotePref());
        final var nodeState = manager.takeNodeState(session.getRemoteAddress(), listener, false);
        final var updater = new SessionStateUpdater(listener, session, nodeState);
        final var topologyNodeIId = TOPO_IID.toBuilder()
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(PcepTopologyNodeStatsAug.class)
            .build();

        // invoke multiple asynchronous updates and reads
        // if synchronization is incorrect race will occur between update and read
        for (int i = 0; i < 100; i++) {
            CompletableFuture.runAsync(() -> updater.updateStatistics());
            CompletableFuture.runAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        }
        // invoke the update once more in way we can wait for the result
        final var update = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        update.get(2, TimeUnit.SECONDS).get(2, TimeUnit.SECONDS);
        // just to make sure assert a successful asynchronous read
        final var node = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        assertEquals(createTopologySessionState(), node.get().get().orElseThrow().getPcepSessionState());

        // invoke multiple asynchronous removes and reads
        // if synchronization is incorrect race will occur between update and read
        for (int i = 0; i < 100; i++) {
            CompletableFuture.runAsync(() -> updater.removeStatistics());
            CompletableFuture.runAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        }

        // invoke the remove once more in way we can wait for the result
        final var remove = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        remove.get(2, TimeUnit.SECONDS).get(2, TimeUnit.SECONDS);
        // just to make sure assert a successful asynchronous read
        final var emptyNode = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        assertEquals(Optional.empty(), emptyNode.get().get());
    }

    private PcepSessionState createTopologySessionState() {
        final var replyTime = new ReplyTimeBuilder()
            .setAverageTime(Uint32.ZERO)
            .setMaxTime(Uint32.ZERO)
            .setMinTime(Uint32.ZERO)
            .build();

        final var errorMsg = new ErrorMessagesBuilder()
            .setLastReceivedError(new LastReceivedErrorBuilder()
                .setErrorType(Uint8.ZERO)
                .setErrorValue(Uint8.ZERO)
                .build())
            .setLastSentError(new LastSentErrorBuilder()
                .setErrorType(Uint8.ZERO)
                .setErrorValue(Uint8.ZERO)
                .build())
            .setReceivedErrorMsgCount(Uint32.ZERO)
            .setSentErrorMsgCount(Uint32.ZERO)
            .build();

        final var statefulMsg = new StatefulMessagesStatsAugBuilder()
            .setLastReceivedRptMsgTimestamp(Uint32.ZERO)
            .setSentUpdMsgCount(Uint32.ZERO)
            .setReceivedRptMsgCount(Uint32.ZERO)
            .setSentInitMsgCount(Uint32.ZERO)
            .build();

        final var messages = new MessagesBuilder()
            .setLastSentMsgTimestamp(Uint32.ZERO)
            .setUnknownMsgReceived(Uint16.ZERO)
            .setSentMsgCount(Uint32.ZERO)
            .setReceivedMsgCount(Uint32.ZERO)
            .setReplyTime(replyTime)
            .setErrorMessages(errorMsg)
            .addAugmentation(statefulMsg).build();

        final var capabilities = new PeerCapabilitiesBuilder()
            .addAugmentation(new StatefulCapabilitiesStatsAugBuilder()
                .setStateful(false)
                .setInstantiation(false)
                .setActive(false)
                .build())
            .build();

        final var localPref = new LocalPrefBuilder()
            .setKeepalive(Uint8.valueOf(10))
            .setDeadtimer(Uint8.valueOf(30))
            .setIpAddress(testAddress)
            .setSessionId(Uint16.ZERO)
            .build();

        return new PcepSessionStateBuilder()
            .setSynchronized(false)
            .setSessionDuration("0:00:00:00")
            .setDelegatedLspsCount(Uint16.ZERO)
            .setLocalPref(localPref)
            .setPeerPref(new PeerPrefBuilder(localPref).build())
            .setPeerCapabilities(capabilities)
            .setMessages(messages)
            .build();
    }
}
