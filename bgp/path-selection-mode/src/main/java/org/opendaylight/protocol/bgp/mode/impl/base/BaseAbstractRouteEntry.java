/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedInteger;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.mode.impl.BGPRouteEntryExportParametersImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
abstract class BaseAbstractRouteEntry extends AbstractRouteEntry<BaseBestPath> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseAbstractRouteEntry.class);
    private OffsetMap offsets = OffsetMap.EMPTY;
    private Attributes[] values = EMPTY_ATTRIBUTES;
    private BaseBestPath bestPath;
    private BaseBestPath removedBestPath;

    BaseAbstractRouteEntry(final BGPPeerTracker peerTracker) {
        super(peerTracker);
    }

    /**
     * Remove route.
     *
     * @param routerId router ID in unsigned integer
     * @param offset   of removed route
     * @return true if its the last route
     */
    protected final boolean removeRoute(final UnsignedInteger routerId, final int offset) {
        this.values = this.offsets.removeValue(this.values, offset, EMPTY_ATTRIBUTES);
        this.offsets = this.offsets.without(routerId);
        return this.offsets.isEmpty();
    }

    @Override
    public final boolean selectBest(final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BasePathSelector selector = new BasePathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < this.offsets.size(); ++i) {
            final UnsignedInteger routerId = this.offsets.getRouterKey(i);
            final Attributes attributes = this.offsets.getValue(this.values, i);
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final BaseBestPath newBestPath = selector.result();
        final boolean modified = newBestPath == null || !newBestPath.equals(this.bestPath);
        if (modified) {
            if (this.offsets.isEmpty()) {
                this.removedBestPath = this.bestPath;
            }
            LOG.trace("Previous best {}, current best {}", this.bestPath, newBestPath);
            this.bestPath = newBestPath;
        }
        return modified;
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final long remotePathId, final Route route) {
        final Attributes advertisedAttrs = route.getAttributes();
        int offset = this.offsets.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            this.values = newOffsets.expand(this.offsets, this.values, offset);
            this.offsets = newOffsets;
        }

        this.offsets.setValue(this.values, offset, advertisedAttrs);
        LOG.trace("Added route from {} attributes {}", routerId, advertisedAttrs);
        return offset;
    }

    @Override
    public void updateBestPaths(
            final RouteEntryDependenciesContainer entryDependencies,
            final String routeKey,
            final WriteTransaction tx) {
        if (this.removedBestPath != null) {
            removePathFromDataStore(entryDependencies, routeKey, tx);
            this.removedBestPath = null;
        }
        if (this.bestPath != null) {
            addPathToDataStore(entryDependencies, routeKey, tx);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeBestPaths(
            final RouteEntryDependenciesContainer entryDep,
            final RouteEntryInfo entryInfo,
            final WriteTransaction tx) {
        if (this.bestPath == null) {
            return;
        }
        final TablesKey localTK = entryDep.getLocalTablesKey();
        final Peer toPeer = entryInfo.getToPeer();
        if (!filterRoutes(this.bestPath.getPeerId(), toPeer, localTK)) {
            return;
        }
        final RIBSupport ribSupport = entryDep.getRibSupport();
        Identifier routeIdentifier = ribSupport.createRouteListKey(this.bestPath.getPathId(), entryInfo.getRouteKey());
        final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(
                this.peerTracker.getPeer(this.bestPath.getPeerId()), toPeer);
        final Optional<Attributes> effAttrib = entryDep.getRoutingPolicies()
                .applyExportPolicies(routeEntry, this.bestPath.getAttributes());
        final Route route = createRoute(ribSupport, entryInfo.getRouteKey(), this.bestPath.getPathId(), this.bestPath);
        InstanceIdentifier ribOutIId = ribSupport.createRouteIdentifier(toPeer.getRibOutIId(localTK), routeIdentifier);
        if (effAttrib.isPresent() && route != null) {
            LOG.debug("Write route {} to peer AdjRibsOut {}", route, toPeer.getPeerId());
            tx.put(LogicalDatastoreType.OPERATIONAL, ribOutIId, route);
            tx.put(LogicalDatastoreType.OPERATIONAL, ribOutIId.child(Attributes.class), effAttrib.get());
        }
    }

    @SuppressWarnings("unchecked")
    private void removePathFromDataStore(final RouteEntryDependenciesContainer entryDep,
            final String routeKey, final WriteTransaction tx) {
        LOG.trace("Best Path removed {}", this.removedBestPath);
        final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget = entryDep.getLocRibTableTarget();
        final RIBSupport ribSup = entryDep.getRibSupport();
        Identifier routeIdentifier = ribSup.createRouteListKey(this.removedBestPath.getPathId(), routeKey);
        final InstanceIdentifier routeTarget = ribSup.createRouteIdentifier(locRibTarget, routeIdentifier);
        LOG.debug("Delete route from LocRib {}", routeTarget);
        tx.delete(LogicalDatastoreType.OPERATIONAL, routeTarget);
        fillAdjRibsOut(null, null, routeIdentifier, this.removedBestPath.getPeerId(),
                entryDep, tx);
    }

    @SuppressWarnings("unchecked")
    private void addPathToDataStore(final RouteEntryDependenciesContainer entryDep,
            final String routeKey, final WriteTransaction tx) {
        final RIBSupport ribSup = entryDep.getRibSupport();
        final Route route = createRoute(ribSup, routeKey, this.bestPath.getPathId(), this.bestPath);
        LOG.trace("Selected best route {}", route);

        Identifier routeIdentifier = ribSup.createRouteListKey(this.bestPath.getPathId(), routeKey);
        final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget = entryDep.getLocRibTableTarget();
        final InstanceIdentifier routeTarget = ribSup.createRouteIdentifier(locRibTarget, routeIdentifier);
        LOG.debug("Write route to LocRib {}", route);
        tx.put(LogicalDatastoreType.OPERATIONAL, routeTarget, route);
        fillAdjRibsOut(this.bestPath.getAttributes(), route, routeIdentifier, this.bestPath.getPeerId(),
                entryDep, tx);
    }

    final OffsetMap getOffsets() {
        return this.offsets;
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    private void fillAdjRibsOut(
            @Nullable final Attributes attributes,
            @Nullable final Route route,
            final Identifier routeKey, final PeerId fromPeerId,
            final RouteEntryDependenciesContainer routeEntryDep,
            final WriteTransaction tx) {
        /*
         * We need to keep track of routers and populate adj-ribs-out, too. If we do not, we need to
         * expose from which client a particular route was learned from in the local RIB, and have
         * the listener perform filtering.
         *
         * We walk the policy set in order to minimize the amount of work we do for multiple peers:
         * if we have two eBGP peers, for example, there is no reason why we should perform the translation
         * multiple times.
         */
        final TablesKey localTK = routeEntryDep.getLocalTablesKey();
        final BGPRibRoutingPolicy routingPolicies = routeEntryDep.getRoutingPolicies();
        final RIBSupport ribSupport = routeEntryDep.getRibSupport();
        for (final Peer toPeer : this.peerTracker.getPeers()) {
            if (!filterRoutes(fromPeerId, toPeer, localTK)) {
                continue;
            }
            Optional<Attributes> effAttr = Optional.empty();
            final Peer fromPeer = this.peerTracker.getPeer(fromPeerId);
            if (fromPeer != null && attributes != null) {
                final BGPRouteEntryExportParameters routeEntry
                        = new BGPRouteEntryExportParametersImpl(fromPeer, toPeer);
                effAttr = routingPolicies.applyExportPolicies(routeEntry, attributes);
            }
            final InstanceIdentifier ribOutTarget
                    = ribSupport.createRouteIdentifier(toPeer.getRibOutIId(localTK), routeKey);
            if (effAttr.isPresent() && route != null) {
                LOG.debug("Write route {} to peer AdjRibsOut {}", route, toPeer.getPeerId());
                tx.put(LogicalDatastoreType.OPERATIONAL, ribOutTarget, route);
                tx.put(LogicalDatastoreType.OPERATIONAL, ribOutTarget.child(Attributes.class), effAttr.get());
            } else {
                LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, toPeer.getPeerId());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
            }
        }
    }
}