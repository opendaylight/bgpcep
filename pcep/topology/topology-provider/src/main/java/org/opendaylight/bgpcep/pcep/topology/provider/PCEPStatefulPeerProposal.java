/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPStatefulPeerProposal implements PCEPPeerProposal {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPStatefulPeerProposal.class);

    private final InstanceIdentifier<Topology> topologyId;
    private final DataBroker dataBroker;

    private volatile SpeakerIdMapping speakerIds;

    PCEPStatefulPeerProposal(final DataBroker dataBroker, final InstanceIdentifier<Topology> topologyId,
            final SpeakerIdMapping speakerIds) {
        this.dataBroker = requireNonNull(dataBroker);
        this.topologyId = requireNonNull(topologyId);
        // FIXME: BGPCEP-989: once we have DTCL, we certainly should be able to maintain this mapping as well
        this.speakerIds = requireNonNull(speakerIds);
    }

    void setSpeakerIds(final SpeakerIdMapping speakerIds) {
        this.speakerIds = requireNonNull(speakerIds);
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

        // FIXME: BGPCEP-989: acquire this information via a DTCL and perform a simple lookup only
        final var addr = address.getAddress();
        Optional<LspDbVersion> result = Optional.empty();

        try (ReadTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            // FIXME: we should be listening for this configuration and keep a proper cache
            final ListenableFuture<Optional<LspDbVersion>> future = rTx.read(LogicalDatastoreType.OPERATIONAL,
                topologyId.child(Node.class, new NodeKey(ServerSessionManager.createNodeId(addr)))
                    .augmentation(Node1.class).child(PathComputationClient.class)
                    .augmentation(PathComputationClient1.class).child(LspDbVersion.class));
            try {
                result = future.get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.warn("Failed to read toplogy {}.", InstanceIdentifier.keyOf(topologyId), e);

            }
        }

        final var speakerId = speakerIds.speakerIdForAddress(addr);
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
}
