/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.pcep.node.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;

final class PCEPStatefulPeerProposal extends AbstractRegistration implements PCEPPeerProposal {
    private abstract static class AbstractListener<D extends DataObject, V> implements DataTreeChangeListener<D> {
        final Map<NodeId, V> map = new ConcurrentHashMap<>();
        final Registration reg;

        @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
            justification = "Stateless specializations in this nest")
        AbstractListener(final DataBroker dataBroker, final @NonNull LogicalDatastoreType datastore,
                final @NonNull DataObjectReference<D> wildcard) {
            reg = dataBroker.registerTreeChangeListener(datastore, wildcard, this);
        }

        final void remove(final DataTreeModification<?> modification) {
            map.remove(extractNodeId(modification));
        }

        final void update(final DataTreeModification<?> modification, final V value) {
            final var nodeId = extractNodeId(modification);
            if (value != null) {
                map.put(nodeId, value);
            } else {
                map.remove(nodeId);
            }
        }

        private static @NonNull NodeId extractNodeId(final DataTreeModification<?> modification) {
            return verifyNotNull(modification.path().firstKeyOf(Node.class)).getNodeId();
        }
    }

    @VisibleForTesting
    static final class SpeakerIdListener extends AbstractListener<SessionConfig, byte[]> {
        SpeakerIdListener(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topologyId) {
            super(dataBroker, LogicalDatastoreType.CONFIGURATION, topologyId.toBuilder().toReferenceBuilder()
                .child(Node.class)
                .augmentation(Node1.class)
                .child(SessionConfig.class)
                .build());
        }

        @Override
        public void onDataTreeChanged(final List<DataTreeModification<SessionConfig>> changes) {
            for (var change : changes) {
                final var config = change.getRootNode().dataAfter();
                if (config != null) {
                    update(change, config.getSpeakerEntityIdValue());
                } else {
                    remove(change);
                }
            }
        }
    }

    @VisibleForTesting
    static final class LspDbVersionListener extends AbstractListener<LspDbVersion, LspDbVersion> {
        LspDbVersionListener(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topologyId) {
            super(dataBroker, LogicalDatastoreType.OPERATIONAL, topologyId.toBuilder().toReferenceBuilder()
                .child(Node.class)
                .augmentation(Node1.class)
                .child(PathComputationClient.class)
                .child(LspDbVersion.class)
                .build());
        }

        @Override
        public void onDataTreeChanged(final List<DataTreeModification<LspDbVersion>> changes) {
            for (var change : changes) {
                update(change, change.getRootNode().dataAfter());
            }
        }
    }

    private final LspDbVersionListener lspDbVersions;
    private final SpeakerIdListener speakerIds;

    PCEPStatefulPeerProposal(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topologyId) {
        lspDbVersions = new LspDbVersionListener(dataBroker, topologyId);
        speakerIds = new SpeakerIdListener(dataBroker, topologyId);
    }

    @Override
    public void setPeerSpecificProposal(final InetSocketAddress address, final TlvsBuilder openBuilder) {
        // Check if we are dealing with synchronization optimization
        final var statefulTlv = openBuilder.getStatefulCapability();
        if (statefulTlv == null) {
            return;
        }

        final var nodeId = ServerSessionManager.createNodeId(address.getAddress());
        final var dbVersion = lspDbVersions.map.get(nodeId);
        final var speakerId = speakerIds.map.get(nodeId);
        if (speakerId == null && dbVersion == null) {
            // Nothing to add
            return;
        }

        openBuilder.setLspDbVersion(dbVersion);
        openBuilder.setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(speakerId).build());
    }

    @Override
    protected void removeRegistration() {
        lspDbVersions.reg.close();
        speakerIds.reg.close();
    }
}
