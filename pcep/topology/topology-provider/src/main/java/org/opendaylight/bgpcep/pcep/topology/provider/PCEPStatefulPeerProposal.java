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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
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

public final class PCEPStatefulPeerProposal {

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

    void setPeerProposal(final NodeId nodeId, final TlvsBuilder openTlvsBuilder) {
        if (isSynOptimizationEnabled(openTlvsBuilder)) {
            try (final ReadOnlyTransaction rTx = this.dataBroker.newReadOnlyTransaction()) {
                final ListenableFuture<Optional<LspDbVersion>> future = rTx.read(
                    LogicalDatastoreType.OPERATIONAL,
                    this.topologyId.child(Node.class, new NodeKey(nodeId)).augmentation(Node1.class)
                    .child(PathComputationClient.class).augmentation(PathComputationClient1.class)
                    .child(LspDbVersion.class));
                Futures.addCallback(future, new FutureCallback<Optional<LspDbVersion>>() {
                    @Override
                    public void onSuccess(final Optional<LspDbVersion> result) {
                        if (result.isPresent()) {
                            openTlvsBuilder.addAugmentation(Tlvs3.class,
                                new Tlvs3Builder().setLspDbVersion(result.get()).build());
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        LOG.warn("Failed to read toplogy {}.", InstanceIdentifier.keyOf(
                            PCEPStatefulPeerProposal.this.topologyId), t);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
    }

    private static boolean isSynOptimizationEnabled(final TlvsBuilder openTlvsBuilder) {
        final Tlvs1 statefulTlv = openTlvsBuilder.getAugmentation(Tlvs1.class);
        if (statefulTlv != null && statefulTlv.getStateful() != null) {
            return statefulTlv.getStateful().getAugmentation(Stateful1.class) != null;
        }
        return false;
    }

}
