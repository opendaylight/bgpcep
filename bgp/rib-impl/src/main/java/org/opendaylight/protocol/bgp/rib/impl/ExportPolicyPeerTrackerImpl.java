/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerExportGroupRegistry;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup;
import org.opendaylight.protocol.bgp.rib.spi.PeerExportGroup.PeerExporTuple;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
final class ExportPolicyPeerTrackerImpl implements ExportPolicyPeerTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ExportPolicyPeerTrackerImpl.class);
    @GuardedBy("this")
    private final Map<YangInstanceIdentifier, PeerRole> peerRoles = new HashMap<>();
    @GuardedBy("this")
    private final Map<PeerId, SendReceive> peerAddPathTables = new HashMap<>();
    @GuardedBy("this")
    private final Set<PeerId> peerTables = new HashSet<>();
    private final PolicyDatabase policyDatabase;
    private final TablesKey localTableKey;
    @GuardedBy("this")
    private final Map<PeerRole, PeerExportGroupRegistry> groups = new EnumMap<>(PeerRole.class);

    ExportPolicyPeerTrackerImpl(final PolicyDatabase policyDatabase, final TablesKey localTablesKey) {
        this.policyDatabase = Preconditions.checkNotNull(policyDatabase);
        this.localTableKey = localTablesKey;
    }

    private synchronized AbstractRegistration addToExportGroups(final PeerId peerId,
        final YangInstanceIdentifier peerPath, final PeerRole peerRole) {
        final PeerExportGroupRegistry peerExp = this.groups.computeIfAbsent(peerRole,
            k -> new PeerExportGroupImpl(this.policyDatabase.exportPolicyForRole(peerRole)));

        final AbstractRegistration registration =  peerExp.registerPeer(peerId, new PeerExporTuple(peerPath, peerRole));

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                registration.close();
                if(ExportPolicyPeerTrackerImpl.this.groups.get(peerRole).isEmpty()) {
                    ExportPolicyPeerTrackerImpl.this.groups.remove(peerRole);
                }
            }
        };
    }

    @Override
    public synchronized AbstractRegistration registerPeer(final PeerId peerId, final SendReceive sendReceive,
        final YangInstanceIdentifier peerPath, final PeerRole peerRole,
        final Optional<SimpleRoutingPolicy> optSimpleRoutingPolicy) {
        if (sendReceive != null) {
            this.peerAddPathTables.put(peerId, sendReceive);
            LOG.debug("Supported Add BestPath table {} added to peer {}", sendReceive, peerId);
        }
        final SimpleRoutingPolicy simpleRoutingPolicy = optSimpleRoutingPolicy.orElse(null);
        if (SimpleRoutingPolicy.AnnounceNone != simpleRoutingPolicy) {
            this.peerTables.add(peerId);
        }
        this.peerRoles.put(peerPath, peerRole);
        LOG.debug("Supported table {} added to peer {} role {}", this.localTableKey, peerId, peerRole);
        final AbstractRegistration registration = addToExportGroups(peerId, peerPath, peerRole);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    final SendReceive sendReceiveValue = ExportPolicyPeerTrackerImpl.this.peerAddPathTables.remove(peerId);
                    if (sendReceiveValue != null) {
                        LOG.debug("Supported Add BestPath table {} removed to peer {}", sendReceiveValue, peerId);
                    }
                    ExportPolicyPeerTrackerImpl.this.peerTables.remove(peerId);
                    LOG.debug("Removed peer {} from supported table {}", peerId, ExportPolicyPeerTrackerImpl.this.localTableKey);
                    ExportPolicyPeerTrackerImpl.this.peerRoles.remove(peerPath);
                    registration.close();
                }
            }
        };
    }

    @Override
    public synchronized PeerExportGroup getPeerGroup(final PeerRole role) {
        return this.groups.get(Preconditions.checkNotNull(role));
    }

    @Override
    public synchronized boolean isTableSupported(final PeerId peerId) {
        return this.peerTables.contains(peerId);
    }

    @Override
    public synchronized PeerRole getRole(final YangInstanceIdentifier peerId) {
        return this.peerRoles.get(peerId);
    }

    @Override
    public synchronized boolean isAddPathSupportedByPeer(final PeerId peerId) {
        final SendReceive sendReceive = this.peerAddPathTables.get(peerId);
        return sendReceive != null && (sendReceive.equals(SendReceive.Both) || sendReceive.equals(SendReceive.Receive));
    }
}