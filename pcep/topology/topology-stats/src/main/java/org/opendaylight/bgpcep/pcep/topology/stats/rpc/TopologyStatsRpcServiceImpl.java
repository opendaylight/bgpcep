/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.stats.rpc;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdRpcAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.PcepEntityIdStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesRpcAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulCapabilitiesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesRpcAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesRpcAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stateful.stats.rev181109.StatefulMessagesStatsAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.grouping.PcepSessionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStatsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.PcepTopologyStatsRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyStatsRpcServiceImpl
        implements PcepTopologyStatsRpcService, ClusteredDataTreeChangeListener<PcepSessionState>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsRpcServiceImpl.class);

    private final DataBroker dataBroker;
    private final ConcurrentMap<InstanceIdentifier<PcepSessionState>, PcepSessionState> sessionStateMap =
            new ConcurrentHashMap<>();
    private ListenerRegistration<TopologyStatsRpcServiceImpl> listenerRegistration;

    public TopologyStatsRpcServiceImpl(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    public synchronized void init() {
        LOG.info("Initializing PCEP Topology Stats RPC service.");
        this.listenerRegistration = this.dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(Node.class)
                                .augmentation(PcepTopologyNodeStatsAug.class).child(PcepSessionState.class).build()),
                this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<PcepSessionState>> changes) {
        changes.forEach(change -> {
            final InstanceIdentifier<PcepSessionState> iid = change.getRootPath().getRootIdentifier();
            final DataObjectModification<PcepSessionState> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    sessionStateMap.put(iid, mod.getDataAfter());
                    break;
                case DELETE:
                    sessionStateMap.remove(iid);
                    break;
                default:
            }
        });
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing PCEP Topology Stats RPC service.");
        if (this.listenerRegistration != null) {
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    @Override
    @SuppressWarnings("checkstyle:LineLength")
    public ListenableFuture<RpcResult<GetStatsOutput>> getStats(final GetStatsInput input) {
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.input.Topology> iTopologies =
                input.getTopology();
        final List<TopologyId> iTopologyIds;
        if (iTopologies != null) {
            iTopologyIds = iTopologies.stream().map(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.input.Topology::getTopologyId)
                    .collect(Collectors.toList());
        } else {
            iTopologyIds = getAvailableTopologyIds();
        }

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.output.Topology> oTopologies =
                new ArrayList<>();
        iTopologyIds.forEach(iTopologyId -> {
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.input.topology.Node> iNodes;
            if (input.getTopology() != null) {
                iNodes = input.getTopology().stream().filter(t -> t.getTopologyId().equals(iTopologyId)).findFirst()
                        .get().getNode();
            } else {
                iNodes = null;
            }

            final List<NodeId> iNodeIds;
            if (iNodes != null) {
                iNodeIds = iNodes.stream().map(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.input.topology.Node::getNodeId)
                        .collect(Collectors.toList());
            } else {
                iNodeIds = getAvailableNodeIds(iTopologyId);
            }

            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.output.topology.Node> oNodes =
                    new ArrayList<>();
            iNodeIds.forEach(iNodeId -> {
                final InstanceIdentifier<PcepSessionState> iid = InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(iTopologyId)).child(Node.class, new NodeKey(iNodeId))
                        .augmentation(PcepTopologyNodeStatsAug.class).child(PcepSessionState.class).build();
                if (!sessionStateMap.containsKey(iid)) {
                    LOG.debug("Pcep session stats not available for node {} in topology {}", iNodeId.getValue(),
                            iTopologyId.getValue());
                }
                oNodes.add(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.output.topology.NodeBuilder()
                                .setNodeId(iNodeId)
                                .setPcepSessionState(transformStatefulAugmentation(sessionStateMap.get(iid))).build());
            });

            oTopologies.add(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.get.stats.output.TopologyBuilder()
                            .setTopologyId(iTopologyId).setNode(oNodes).build());
        });

        final RpcResult<GetStatsOutput> res =
                SuccessfulRpcResult.create(new GetStatsOutputBuilder().setTopology(oTopologies).build());
        return Futures.immediateFuture(res);
    }

    /*
     * Replace stateful topology augmentations with ones for rpc in PCEP session
     * stats data
     */
    private static PcepSessionState transformStatefulAugmentation(final PcepSessionState pcepSessionState) {
        if (pcepSessionState == null) {
            return null;
        }

        final PcepSessionStateBuilder sb = new PcepSessionStateBuilder(pcepSessionState);

        final Messages topoMessage = pcepSessionState.getMessages();
        if (topoMessage != null) {
            final StatefulMessagesStatsAug messageStatsAug = topoMessage.augmentation(StatefulMessagesStatsAug.class);
            if (messageStatsAug != null) {
                final StatefulMessagesRpcAug messageRpcAug = new StatefulMessagesRpcAugBuilder()
                        .setLastReceivedRptMsgTimestamp(messageStatsAug.getLastReceivedRptMsgTimestamp())
                        .setReceivedRptMsgCount(messageStatsAug.getReceivedRptMsgCount())
                        .setSentInitMsgCount(messageStatsAug.getSentInitMsgCount())
                        .setSentUpdMsgCount(messageStatsAug.getSentUpdMsgCount()).build();
                sb.setMessages(new MessagesBuilder(topoMessage).removeAugmentation(StatefulMessagesStatsAug.class)
                        .addAugmentation(StatefulMessagesRpcAug.class, messageRpcAug).build());
            }
        }

        final PeerCapabilities topoPeerCapability = pcepSessionState.getPeerCapabilities();
        if (topoPeerCapability != null) {
            final StatefulCapabilitiesStatsAug capabilityStatsAug =
                    topoPeerCapability.augmentation(StatefulCapabilitiesStatsAug.class);
            if (capabilityStatsAug != null) {
                final StatefulCapabilitiesRpcAug capabilityRpcAug = new StatefulCapabilitiesRpcAugBuilder()
                        .setActive(capabilityStatsAug.isActive()).setInstantiation(capabilityStatsAug.isInstantiation())
                        .setStateful(capabilityStatsAug.isStateful()).build();
                sb.setPeerCapabilities(new PeerCapabilitiesBuilder(topoPeerCapability)
                        .removeAugmentation(StatefulCapabilitiesStatsAug.class)
                        .addAugmentation(StatefulCapabilitiesRpcAug.class, capabilityRpcAug).build());
            }
        }

        final LocalPref topoLocalPref = pcepSessionState.getLocalPref();
        if (topoLocalPref != null) {
            final PcepEntityIdStatsAug entityStatsAug = topoLocalPref.augmentation(PcepEntityIdStatsAug.class);
            if (entityStatsAug != null) {
                final PcepEntityIdRpcAug entityRpcAug = new PcepEntityIdRpcAugBuilder()
                        .setSpeakerEntityIdValue(entityStatsAug.getSpeakerEntityIdValue()).build();
                sb.setLocalPref(new LocalPrefBuilder(topoLocalPref).removeAugmentation(PcepEntityIdStatsAug.class)
                        .addAugmentation(PcepEntityIdRpcAug.class, entityRpcAug).build());
            }
        }

        return sb.build();
    }

    private List<TopologyId> getAvailableTopologyIds() {
        return sessionStateMap.keySet().stream().map(iid -> iid.firstKeyOf(Topology.class).getTopologyId()).distinct()
                .collect(Collectors.toList());
    }

    private List<NodeId> getAvailableNodeIds(final TopologyId topologyId) {
        return sessionStateMap.keySet().stream()
                .filter(iid -> iid.firstKeyOf(Topology.class).getTopologyId().equals(topologyId))
                .map(iid -> iid.firstKeyOf(Node.class).getNodeId()).collect(Collectors.toList());
    }
}
