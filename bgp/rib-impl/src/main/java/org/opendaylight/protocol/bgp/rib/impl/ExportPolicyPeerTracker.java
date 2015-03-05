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
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks peers for adj-rib-out writeout.
 */
final class ExportPolicyPeerTracker extends AbstractPeerRoleTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ExportPolicyPeerTracker.class);
    private static final Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>> GENERATE_PEERID =
            new Function<YangInstanceIdentifier, Entry<PeerId, YangInstanceIdentifier>>() {
        @Override
        public Entry<PeerId, YangInstanceIdentifier> apply(final YangInstanceIdentifier input) {
            final PeerId peerId = IdentifierUtils.peerId((NodeIdentifierWithPredicates) input.getLastPathArgument());
            return new AbstractMap.SimpleImmutableEntry<>(peerId, input);
        }
    };

    private final Map<YangInstanceIdentifier, PeerRole> peerRoles = new HashMap<>();
    private volatile Map<PeerRole, PeerExportGroup> groups = Collections.emptyMap();

    protected ExportPolicyPeerTracker(final DOMDataTreeChangeService service, final YangInstanceIdentifier ribId) {
        super(service, ribId);
    }

    private static Map<PeerRole, PeerExportGroup> createGroups(final Map<YangInstanceIdentifier, PeerRole> peerPathRoles) {
        if (peerPathRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        // Index things nicely for easy access
        final Multimap<PeerRole, YangInstanceIdentifier> roleToIds = ArrayListMultimap.create(PeerRole.values().length, 2);
        final Map<PeerId, PeerRole> idToRole = new HashMap<>();
        for (Entry<YangInstanceIdentifier, PeerRole> e : peerPathRoles.entrySet()) {
            roleToIds.put(e.getValue(), e.getKey());
            idToRole.put(IdentifierUtils.peerId((NodeIdentifierWithPredicates) e.getKey().getLastPathArgument()), e.getValue());
        }

        // Optimized immutable copy, reused for all PeerGroups
        final Map<PeerId, PeerRole> peerRoles = ImmutableMap.copyOf(idToRole);

        final Map<PeerRole, PeerExportGroup> ret = new EnumMap<>(PeerRole.class);
        for (Entry<PeerRole, Collection<YangInstanceIdentifier>> e : roleToIds.asMap().entrySet()) {
            final AbstractExportPolicy policy = AbstractExportPolicy.forRole(e.getKey());
            final Collection<Entry<PeerId, YangInstanceIdentifier>> peers = ImmutableList.copyOf(Collections2.transform(e.getValue(), GENERATE_PEERID));

            ret.put(e.getKey(), new PeerExportGroup(peers, peerRoles, policy));
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
            oldRole = peerRoles.put(peerPath, role);
        } else {
            oldRole = peerRoles.remove(peerPath);
        }

        if (role != oldRole) {
            LOG.debug("Peer {} changed role from {} to {}", peerPath, oldRole, role);
            groups = createGroups(peerRoles);
        }
    }

    PeerExportGroup getPeerGroup(final PeerRole role) {
        return groups.get(Preconditions.checkNotNull(role));
    }
}