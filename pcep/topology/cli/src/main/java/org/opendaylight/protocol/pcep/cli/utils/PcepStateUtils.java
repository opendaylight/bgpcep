/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.cli.utils;

import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import org.apache.karaf.shell.table.ShellTable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulCapabilitiesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev171113.StatefulMessagesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.Error;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.Preferences;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.reply.time.grouping.ReplyTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev171113.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PcepStateUtils reads Pcep Topology Node state from DS and prints to stream.
 */
public final class PcepStateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PcepStateUtils.class);

    @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
    private PcepStateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Display to stream operational state, rib Id is mandatory.
     *
     * @param dataBroker data broker
     * @param stream     where to print
     * @param topologyId mandatory, Topology where Node pertains
     * @param nodeId     mandatory, State per given Node Id will be printed
     */
    public static void displayNodeState(@NonNull final DataBroker dataBroker,
            @NonNull final PrintStream stream, @NonNull final String topologyId, @NonNull final String nodeId) {
        final Node node = readNodeFromDataStore(dataBroker, topologyId, nodeId);
        if (node == null) {
            stream.println(String.format("Node [%s] not found", nodeId));
            return;
        }
        final PcepTopologyNodeStatsAug state = node.getAugmentation(PcepTopologyNodeStatsAug.class);
        if (state == null) {
            stream.println(String.format("State not found for [%s]", nodeId));
            return;
        }
        final PcepSessionState nodeState = state.getPcepSessionState();
        displayNodeState(topologyId, nodeId, nodeState, stream);
    }

    private static void displayNodeState(
            final String topologyId,
            final String nodeId,
            final PcepSessionState pcepSessionState,
            final PrintStream stream) {
        final ShellTable table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();

        showNodeState(table, topologyId, nodeId, pcepSessionState);

        addHeader(table, "Local preferences");
        final LocalPref localPref = pcepSessionState.getLocalPref();
        showPreferences(table, localPref);

        addHeader(table, "Peer preferences");
        final PeerPref peerPref = pcepSessionState.getPeerPref();
        showPreferences(table, peerPref);

        showCapabilities(table, pcepSessionState.getPeerCapabilities());

        final Messages messages = pcepSessionState.getMessages();
        showMessages(table, messages);

        final ErrorMessages error = messages.getErrorMessages();
        showErrorMessages(table, error);

        final ReplyTime reply = messages.getReplyTime();
        showReplyMessages(table, reply);

        table.print(stream);
    }

    private static void showNodeState(final ShellTable table, final String topologyId, final String nodeId,
            final PcepSessionState pcepSessionState) {
        addHeader(table, "Node state");
        table.addRow().addContent("Topology Id", topologyId);
        table.addRow().addContent("Node Id", nodeId);
        table.addRow().addContent("Session duration", pcepSessionState.getSessionDuration());
        table.addRow().addContent("Synchronized", pcepSessionState.isSynchronized());
        table.addRow().addContent("Delegated Lsp Count", pcepSessionState.getDelegatedLspsCount());
    }

    private static void showCapabilities(final ShellTable table, final PeerCapabilities capa) {
        if (capa == null) {
            return;
        }
        final StatefulCapabilitiesStatsAug stateFulCapa = capa.getAugmentation(StatefulCapabilitiesStatsAug.class);
        if (stateFulCapa != null) {
            addHeader(table, "Stateful Capabilities");
            table.addRow().addContent("Stateful", stateFulCapa.isStateful());
            table.addRow().addContent("Active", stateFulCapa.isActive());
            table.addRow().addContent("Instantiation", stateFulCapa.isInstantiation());
        }

    }

    private static void showMessages(final ShellTable table, final Messages messages) {
        if (messages == null) {
            return;
        }
        addHeader(table, "Messages");
        table.addRow().addContent("Last Sent Msg Timestamp", messages.getLastSentMsgTimestamp());
        table.addRow().addContent("Received Msg Count", messages.getReceivedMsgCount());
        table.addRow().addContent("Sent Msg Count", messages.getSentMsgCount());
        table.addRow().addContent("Unknown Msg Received", messages.getUnknownMsgReceived());

        final StatefulMessagesStatsAug statefulMessages = messages.getAugmentation(StatefulMessagesStatsAug.class);
        if (statefulMessages == null) {
            return;
        }
        addHeader(table, " Stateful Messages");
        table.addRow().addContent("Last Received RptMsg Timestamp", statefulMessages
                .getLastReceivedRptMsgTimestamp());
        table.addRow().addContent("Received RptMsg", statefulMessages.getReceivedRptMsgCount());
        table.addRow().addContent("Sent Init Msg", statefulMessages.getSentInitMsgCount());
        table.addRow().addContent("Sent Upd Msg", statefulMessages.getSentUpdMsgCount());
    }

    private static void showReplyMessages(final ShellTable table, final ReplyTime reply) {
        if (reply == null) {
            return;
        }
        addHeader(table, "Reply Messages");
        table.addRow().addContent("Average Time", reply.getAverageTime());
        table.addRow().addContent("Max Timet", reply.getMaxTime());
        table.addRow().addContent("Min Time", reply.getMinTime());
    }

    private static void showErrorMessages(final ShellTable table, final ErrorMessages error) {
        if (error == null) {
            return;
        }
        addHeader(table, "Error Messages");
        table.addRow().addContent("Sent Error Msg Count", error.getSentErrorMsgCount());
        table.addRow().addContent("Received Error Msg Count", error.getReceivedErrorMsgCount());

        showError(table, error.getLastSentError(), "Last Sent Error");
        showError(table, error.getLastReceivedError(), "Last Received Error");
    }

    private static void showError(final ShellTable table, final Error error, final String errorMsgHeader) {
        if (error == null) {
            return;
        }
        addHeader(table, errorMsgHeader);
        table.addRow().addContent("Type", error.getErrorType());
        table.addRow().addContent("Value", error.getErrorValue());

    }

    private static void showPreferences(final ShellTable table, final Preferences preferences) {
        table.addRow().addContent("Session id", preferences.getSessionId());
        table.addRow().addContent("Ip Address", preferences.getIpAddress());
        table.addRow().addContent("Dead Timer", preferences.getDeadtimer());
        table.addRow().addContent("Keep Alive", preferences.getKeepalive());
    }

    private static Node readNodeFromDataStore(final DataBroker dataBroker,
            final String topologyId, final String nodeId) {
        final InstanceIdentifier<Node> topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
                .child(Node.class, new NodeKey(new NodeId(nodeId))).build();

        final ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();

        try {
            return rot.read(LogicalDatastoreType.OPERATIONAL, topology).get().orNull();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to read node {}", nodeId, e);
        }
        return null;
    }


    private static void addHeader(final ShellTable table, final String header) {
        table.addRow().addContent("                      ", "");
        table.addRow().addContent(header, "");
        table.addRow().addContent("======================", "");
    }
}
