/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

// FIXME: instantiate for each table, listen on wildcard peer and routes
@NotThreadSafe
final class LocRibWriter implements DOMDataTreeChangeListener {
    private final Map<PathArgument, RouteEntry> routeEntries = new HashMap<>();
    private final YangInstanceIdentifier target;
    private final DOMTransactionChain chain;
    private final RIBSupport ribSupport;
    private final Long ourAs;

    // FIXME: these maps need to be populated
    private final Map<PeerRole, Map<PeerId, YangInstanceIdentifier>> peersToUpdate = new EnumMap<>(PeerRole.class);
    private final Map<PeerId, PeerRole> peers = new HashMap<>();

    LocRibWriter(final RIBSupport ribSupport, final DOMTransactionChain chain, final YangInstanceIdentifier target, final Long ourAs) {
        this.chain = Preconditions.checkNotNull(chain);
        this.target = Preconditions.checkNotNull(target);
        this.ourAs = Preconditions.checkNotNull(ourAs);
        this.ribSupport = Preconditions.checkNotNull(ribSupport);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        /*
         * We use two-stage processing here in hopes that we avoid duplicate
         * calculations when multiple peers have changed a particular entry.
         */
        final Map<RouteUpdateKey, RouteEntry> toUpdate = new HashMap<>();
        for (DataTreeCandidate tc : changes) {
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument routeId = path.getLastPathArgument();
            final NodeIdentifierWithPredicates peerKey = EffectiveRibInWriter.peerKey(path);
            final PeerId peerId = EffectiveRibInWriter.peerId(peerKey);
            final UnsignedInteger routerId = RouterIds.routerIdForPeerId(peerId);

            RouteEntry entry = routeEntries.get(routeId);
            if (tc.getRootNode().getDataAfter().isPresent()) {
                if (entry == null) {
                    entry = new RouteEntry();
                    routeEntries.put(routeId, entry);
                }

                entry.addRoute(routerId, (ContainerNode) tc.getRootNode().getDataAfter().get());
            } else if (entry != null) {
                if (entry.removeRoute(routerId)) {
                    routeEntries.remove(routeId);
                    entry = null;
                }
            }

            toUpdate.put(new RouteUpdateKey(peerId, routeId), entry);
        }

        final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

        // Now walk all updated entries
        for (Entry<RouteUpdateKey, RouteEntry> e : toUpdate.entrySet()) {
            final RouteEntry entry = e.getValue();
            final NormalizedNode<?, ?> value;

            if (entry != null) {
                if (!entry.selectBest(ourAs)) {
                    // Best path has not changed, no need to do anything else. Proceed to next route.
                    continue;
                }

                value = entry.bestValue(e.getKey().getRouteId());
            } else {
                value = null;
            }

            if (value != null) {
                tx.put(LogicalDatastoreType.OPERATIONAL, target.node(e.getKey().getRouteId()), value);
            } else {
                tx.delete(LogicalDatastoreType.OPERATIONAL, target.node(e.getKey().getRouteId()));
            }

            /*
             * We need to keep track of routers and populate adj-ribs-out, too. If we do not, we need to
             * expose from which client a particular route was learned from in the local RIB, and have
             * the listener perform filtering.
             *
             * We walk the policy set in order to minimize the amount of work we do for multiple peers:
             * if we have two eBGP peers, for example, there is no reason why we should perform the translation
             * multiple times.
             */
            for (Entry<PeerRole, AbstractExportPolicy> pe : AbstractExportPolicy.POLICIES.entrySet()) {
                final Map<PeerId, YangInstanceIdentifier> toPeers = peersToUpdate.get(pe.getKey());
                if (toPeers == null || toPeers.isEmpty()) {
                    continue;
                }

                final ContainerNode attributes = null;
                final PeerId peerId = e.getKey().getPeerId();
                final ContainerNode effectiveAttributes = pe.getValue().effectiveAttributes(peers.get(peerId), attributes);

                for (Entry<PeerId, YangInstanceIdentifier> pid : toPeers.entrySet()) {
                    // This points to adj-rib-out for a particlar peer/table combination
                    final YangInstanceIdentifier routeTarget = pid.getValue().node(e.getKey().getRouteId());

                    if (effectiveAttributes != null && value != null && !peerId.equals(pid.getKey())) {
                        tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, value);
                        tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget.node(ribSupport.routeAttributes()), effectiveAttributes);
                    } else {
                        tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
                    }
                }
            }
        }

        tx.submit();
    }
}
