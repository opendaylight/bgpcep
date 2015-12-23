/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ExportPolicy;
import org.opendaylight.protocol.bgp.rib.impl.spi.PolicyDatabase;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks peers for adj-rib-out writeout.
 */
final class ExportPolicyPeerTracker extends AbstractPeerRoleTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ExportPolicyPeerTracker.class);
    private static final Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>> GENERATE_PEERID = new Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>>() {
        @Override
        public Entry<PeerId, YangInstanceIdentifier> apply(final YangInstanceIdentifier input) {
            final PeerId peerId = IdentifierUtils.peerId((NodeIdentifierWithPredicates) input.getLastPathArgument());
            return new AbstractMap.SimpleImmutableEntry<>(peerId, input);
        }
    };

    private final Map<YangInstanceIdentifier, PeerRole> peerRoles = new HashMap<>();
    private final HashMultimap<PeerId, NodeIdentifierWithPredicates> peerTables = HashMultimap.create();
    private volatile Map<PeerRole, PeerExportGroup> groups = Collections.emptyMap();
    private final PolicyDatabase policyDatabase;

    ExportPolicyPeerTracker(final PolicyDatabase policyDatabase) {
        this.policyDatabase = Preconditions.checkNotNull(policyDatabase);
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
            final ExportPolicy policy = this.policyDatabase.exportPolicyForRole(e.getKey());
            final Collection<Entry<PeerId, YangInstanceIdentifier>> peers = ImmutableList.copyOf(Collections2.transform(e.getValue(), GENERATE_PEERID));

            ret.put(e.getKey(), new PeerExportGroup(peers, allPeerRoles, policy));
        }

        return ret;
    }

    @Override
    protected void peerRoleChanged(final YangInstanceIdentifier peerPath, final PeerRole role) {
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

    PeerExportGroup getPeerGroup(final PeerRole role) {
        return this.groups.get(Preconditions.checkNotNull(role));
    }

    void onTablesChanged(final DataTreeCandidateNode change, final YangInstanceIdentifier peerPath) {
        final PeerId peerId = IdentifierUtils.peerId((NodeIdentifierWithPredicates) peerPath.getLastPathArgument());
        for (final DataTreeCandidateNode node : change.getChildNodes()) {
            if (node.getDataAfter().isPresent()) {
                final NodeIdentifierWithPredicates value = (NodeIdentifierWithPredicates) node.getDataAfter().get().getIdentifier();
                final boolean added = this.peerTables.put(peerId, value);
                if (added) {
                    LOG.debug("Supported table {} added to peer {}", value, peerId);
                }
            } else {
                LOG.debug("Removed tables {} from peer {}", this.peerTables.removeAll(peerId), peerId);
            }
        }
    }

    boolean isTableSupported(final PeerId peerId, final TablesKey tablesKey) {
        return this.peerTables.get(peerId).contains(RibSupportUtils.toYangKey(SupportedTables.QNAME, tablesKey));
    }
}