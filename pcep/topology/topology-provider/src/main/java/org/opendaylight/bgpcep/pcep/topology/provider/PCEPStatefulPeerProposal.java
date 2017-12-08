/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPStatefulPeerProposal {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPStatefulPeerProposal.class);

    private final DataBroker dataBroker;
    private final InstanceIdentifier<Topology> topologyId;

    private PCEPStatefulPeerProposal(final DataBroker dataBroker, final InstanceIdentifier<Topology> topologyId) {
        this.dataBroker = requireNonNull(dataBroker);
        this.topologyId = requireNonNull(topologyId);
    }

    public static PCEPStatefulPeerProposal createStatefulPeerProposal(final DataBroker dataBroker,
            final InstanceIdentifier<Topology> topologyId) {
        return new PCEPStatefulPeerProposal(dataBroker, topologyId);
    }

    private static boolean isSynOptimizationEnabled(final TlvsBuilder openTlvsBuilder) {
        final Tlvs1 statefulTlv = openTlvsBuilder.getAugmentation(Tlvs1.class);
        if (statefulTlv != null && statefulTlv.getStateful() != null) {
            return statefulTlv.getStateful().getAugmentation(Stateful1.class) != null;
        }
        return false;
    }

    void setPeerProposal(final NodeId nodeId, final TlvsBuilder openTlvsBuilder, final byte[] speakerId) {
        if (isSynOptimizationEnabled(openTlvsBuilder)) {
            Optional<LspDbVersion> result = Optional.absent();
            try (final ReadOnlyTransaction rTx = this.dataBroker.newReadOnlyTransaction()) {
                final ListenableFuture<Optional<LspDbVersion>> future = rTx.read(
                        LogicalDatastoreType.OPERATIONAL,
                        this.topologyId.child(Node.class, new NodeKey(nodeId)).augmentation(Node1.class)
                                .child(PathComputationClient.class).augmentation(PathComputationClient1.class)
                                .child(LspDbVersion.class));
                try {
                    result = future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    LOG.warn("Failed to read toplogy {}.", InstanceIdentifier.keyOf(
                            PCEPStatefulPeerProposal.this.topologyId), e);

                }
            }
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
            openTlvsBuilder.addAugmentation(Tlvs3.class, syncBuilder.build()).build();
        }
    }

}
