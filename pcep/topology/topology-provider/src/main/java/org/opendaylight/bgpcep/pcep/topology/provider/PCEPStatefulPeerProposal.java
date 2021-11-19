/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev200120.pcep.node.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.config.rev181109.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.sync.optimizations.config.rev181109.PcepNodeSyncConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPStatefulPeerProposal extends AbstractRegistration implements PCEPPeerProposal {
    private static final class SpeakerIdListener implements DataTreeChangeListener<PcepNodeSyncConfig> {
        final Map<NodeId, byte[]> map = new ConcurrentHashMap<>();
        final Registration reg;

        SpeakerIdListener(final DataBroker dataBroker, final InstanceIdentifier<Topology> topologyId) {
            reg = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
                LogicalDatastoreType.CONFIGURATION, topologyId.child(Node.class).augmentation(PcepNodeConfig.class)
                    .child(SessionConfig.class).augmentation(PcepNodeSyncConfig.class)), this);
        }

        @Override
        public void onDataTreeChanged(final Collection<DataTreeModification<PcepNodeSyncConfig>> changes) {
            for (var change : changes) {
                final var nodeId = verifyNotNull(change.getRootPath().getRootIdentifier().firstKeyOf(Node.class))
                    .getNodeId();
                final var config = change.getRootNode().getDataAfter();
                if (config != null) {
                    final var speakerEntityId = config.getSpeakerEntityIdValue();
                    if (speakerEntityId != null) {
                        map.put(nodeId, speakerEntityId);
                        continue;
                    }
                }
                map.remove(nodeId);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPStatefulPeerProposal.class);

    private final InstanceIdentifier<Topology> topologyId;
    private final SpeakerIdListener speakerIds;
    private final DataBroker dataBroker;

    PCEPStatefulPeerProposal(final DataBroker dataBroker, final InstanceIdentifier<Topology> topologyId) {
        this.dataBroker = requireNonNull(dataBroker);
        this.topologyId = requireNonNull(topologyId);
        speakerIds = new SpeakerIdListener(dataBroker, topologyId);
    }

    @Override
    public void setPeerSpecificProposal(final InetSocketAddress address, final TlvsBuilder openBuilder) {
        // Check if we are dealing with synchronization optimization
        final var statefulTlv = openBuilder.augmentation(Tlvs1.class);
        if (statefulTlv == null) {
            return;
        }
        final var stateful = statefulTlv.getStateful();
        if (stateful == null || stateful.augmentation(Stateful1.class) == null) {
            return;
        }

        final var addr = address.getAddress();
        final var nodeId = ServerSessionManager.createNodeId(addr);
        // FIXME: BGPCEP-989: acquire this information via a DTCL and perform a simple lookup only
        Optional<LspDbVersion> result = Optional.empty();
        try (ReadTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            // FIXME: we should be listening for this configuration and keep a proper cache
            final ListenableFuture<Optional<LspDbVersion>> future = rTx.read(LogicalDatastoreType.OPERATIONAL,
                topologyId.child(Node.class, new NodeKey(nodeId))
                    .augmentation(Node1.class).child(PathComputationClient.class)
                    .augmentation(PathComputationClient1.class).child(LspDbVersion.class));
            try {
                result = future.get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.warn("Failed to read toplogy {}.", InstanceIdentifier.keyOf(topologyId), e);
            }
        }

        final var speakerId = speakerIds.map.get(nodeId);
        if (speakerId == null && !result.isPresent()) {
            return;
        }
        final Tlvs3Builder syncBuilder = new Tlvs3Builder();

        if (result.isPresent()) {
            syncBuilder.setLspDbVersion(result.get());
        }
        if (speakerId != null) {
            syncBuilder.setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(speakerId).build());
        }
        openBuilder.addAugmentation(syncBuilder.build()).build();
    }

    @Override
    protected void removeRegistration() {
        speakerIds.reg.close();
    }
}
