/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExportPolicyPeerTrackerImpl implements ExportPolicyPeerTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ExportPolicyPeerTrackerImpl.class);
    private static final Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>> GENERATE_PEER_ID = new Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>>() {
        @Override
        public Entry<PeerId, YangInstanceIdentifier> apply(final YangInstanceIdentifier input) {
            final PeerId peerId = IdentifierUtils.peerId((NodeIdentifierWithPredicates) input.getLastPathArgument());
            return new AbstractMap.SimpleImmutableEntry<>(peerId, input);
        }
    };
    private static final QName SEND_RECEIVE = QName.create(SupportedTables.QNAME, "send-receive").intern();
    private static final NodeIdentifier SEND_RECEIVE_NID = new NodeIdentifier(SEND_RECEIVE);
    private final Map<YangInstanceIdentifier, PeerRole> peerRoles = new HashMap<>();
    private final Set<PeerId> peerTables = Sets.newHashSet();
    private final PolicyDatabase policyDatabase;
    private final Map<PeerId, SendReceive> peerAddPathTables = new HashMap<>();
    private final TablesKey localTableKey;
    private volatile Map<PeerRole, PeerExportGroup> groups = Collections.emptyMap();

    ExportPolicyPeerTrackerImpl(final PolicyDatabase policyDatabase, final TablesKey localTablesKey) {
        this.policyDatabase = Preconditions.checkNotNull(policyDatabase);
        this.localTableKey = localTablesKey;
    }

    private Map<PeerRole, PeerExportGroup> createGroups(final Map<YangInstanceIdentifier, PeerRole> peerPathRoles) {
        if (peerPathRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        // Index things nicely for easy access
        final Multimap<PeerRole, YangInstanceIdentifier> roleToIds = ArrayListMultimap.create(PeerRole.values().length, 2);
        final Map<PeerId, PeerRole> idToRole = new HashMap<>();
        for (final Entry<YangInstanceIdentifier, PeerRole> e : peerPathRoles.entrySet()) {
            roleToIds.put(e.getValue(), e.getKey());
            idToRole.put(IdentifierUtils.peerId((NodeIdentifierWithPredicates) e.getKey().getLastPathArgument()), e.getValue());
        }

        // Optimized immutable copy, reused for all PeerGroups
        final Map<PeerId, PeerRole> allPeerRoles = ImmutableMap.copyOf(idToRole);

        final Map<PeerRole, PeerExportGroup> ret = new EnumMap<>(PeerRole.class);
        for (final Entry<PeerRole, Collection<YangInstanceIdentifier>> e : roleToIds.asMap().entrySet()) {
            final AbstractExportPolicy policy = this.policyDatabase.exportPolicyForRole(e.getKey());
            final Collection<Entry<PeerId, YangInstanceIdentifier>> peers = ImmutableList.copyOf(Collections2.transform(e.getValue(), GENERATE_PEER_ID));

            ret.put(e.getKey(), new PeerExportGroupImpl(peers, allPeerRoles, policy));
        }

        return ret;
    }

    @Override
    public void peerRoleChanged(@Nonnull final YangInstanceIdentifier peerPath, @Nullable final PeerRole role) {
        /*
         * This is a sledgehammer approach to the problem: modify the role map first,
         * then construct the group map from scratch.
         */
        final PeerRole oldRole;
        if (role != null) {
            oldRole = this.peerRoles.put(peerPath, role);
        } else {
            oldRole = this.peerRoles.remove(peerPath);
        }

        if (role != oldRole) {
            LOG.debug("Peer {} changed role from {} to {}", peerPath, oldRole, role);
            this.groups = createGroups(this.peerRoles);
        }
    }

    @Override
    public void onTablesChanged(final PeerId peerId, final DataTreeCandidateNode tablesChange) {
        final NodeIdentifierWithPredicates supTablesKey = RibSupportUtils.toYangKey(SupportedTables.QNAME, this.localTableKey);
        final DataTreeCandidateNode localTableNode = tablesChange.getModifiedChild(supTablesKey);
        if (localTableNode != null) {
            final Optional<NormalizedNode<?, ?>> dataAfter = localTableNode.getDataAfter();
            processSupportedSendReceiveTables(localTableNode.getModifiedChild(SEND_RECEIVE_NID), peerId);
            if (dataAfter.isPresent()) {
                final boolean added = this.peerTables.add(peerId);
                if (added) {
                    LOG.debug("Supported table {} added to peer {}", this.localTableKey, peerId);
                }
            } else {
                final NodeIdentifierWithPredicates value = (NodeIdentifierWithPredicates) localTableNode.getIdentifier();
                this.peerTables.remove(peerId);
                LOG.debug("Removed tables {} from peer {}", value, peerId);
            }
        }
    }

    @Override
    public PeerExportGroup getPeerGroup(final PeerRole role) {
        return this.groups.get(Preconditions.checkNotNull(role));
    }

    @Override
    public PeerRole getRole(final YangInstanceIdentifier peerId) {
        return this.peerRoles.get(peerId);
    }

    private void processSupportedSendReceiveTables(final DataTreeCandidateNode sendReceiveModChild, final PeerId peerId) {
        if (sendReceiveModChild != null) {
            if (sendReceiveModChild.getModificationType().equals(ModificationType.DELETE)) {
                final Optional<NormalizedNode<?, ?>> sendReceiveNode = sendReceiveModChild.getDataBefore();
                if (sendReceiveNode.isPresent()) {
                    final SendReceive sendReceiveValue = SendReceive.valueOf(BindingMapping.getClassName((String) sendReceiveNode.get().getValue()));
                    this.peerAddPathTables.remove(peerId);
                    LOG.debug("Supported Add BestPath table {} removed to peer {}", sendReceiveValue, peerId);
                }
            } else {
                final Optional<NormalizedNode<?, ?>> sendReceiveNode = sendReceiveModChild.getDataAfter();
                if (sendReceiveNode.isPresent()) {
                    final SendReceive sendReceiveValue = SendReceive.valueOf(BindingMapping.getClassName((String) sendReceiveNode.get().getValue()));
                    this.peerAddPathTables.put(peerId, sendReceiveValue);
                    LOG.debug("Supported Add BestPath table {} added to peer {}", sendReceiveValue, peerId);
                }
            }
        }
    }

    @Override
    public boolean isTableSupported(final PeerId peerId) {
        return this.peerTables.contains(peerId);
    }

    @Override
    public boolean isAddPathSupportedByPeer(final PeerId peerId) {
        final SendReceive sendReceive = this.peerAddPathTables.get(peerId);
        return sendReceive != null && (sendReceive.equals(SendReceive.Both) || sendReceive.equals(SendReceive.Receive));
    }
}