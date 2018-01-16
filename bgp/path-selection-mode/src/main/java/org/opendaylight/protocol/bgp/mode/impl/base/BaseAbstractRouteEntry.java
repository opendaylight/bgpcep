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
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.mode.impl.BGPRouteEntryExportParametersImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
abstract class BaseAbstractRouteEntry extends AbstractRouteEntry<BaseBestPath> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseAbstractRouteEntry.class);
    private static final ContainerNode[] EMPTY_ATTRIBUTES = new ContainerNode[0];
    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
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
        this.values = this.offsets.removeValue(this.values, offset);
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
            final ContainerNode attributes = this.offsets.getValue(this.values, i);
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
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId,
            final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs
                = (ContainerNode) NormalizedNodes
                .findNode(data, attributesIdentifier).orElse(null);
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
    public void updateBestPaths(final RouteEntryDependenciesContainer entryDependencies,
            final NodeIdentifierWithPredicates routeIdPA,
            final DOMDataWriteTransaction tx) {
        if (this.removedBestPath != null) {
            removePathFromDataStore(entryDependencies, routeIdPA, tx);
            this.removedBestPath = null;
        }
        if (this.bestPath != null) {
            addPathToDataStore(entryDependencies, routeIdPA, tx);
        }
    }

    @Override
    public void initializeBestPaths(final RouteEntryDependenciesContainer entryDep,
            final RouteEntryInfo entryInfo, final DOMDataWriteTransaction tx) {
        if (this.bestPath != null) {
            final BaseBestPath path = this.bestPath;
            final PeerId destPeer = entryInfo.getToPeerId();
            final PeerRole destPeerRole = this.peerTracker.getRole(destPeer);
            if (filterRoutes(path.getPeerId(), destPeer, entryDep.getLocalTablesKey())) {
                final NodeIdentifierWithPredicates routeId = entryInfo.getRouteId();
                final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(
                        entryInfo.getRouteId(), path.getPeerId(),
                        this.peerTracker.getRole(path.getPeerId()), destPeer, destPeerRole);
                final Optional<ContainerNode> effAttrib = entryDep.getRoutingPolicies()
                        .applyExportPolicies(routeEntry, path.getAttributes());
                writeRoute(destPeer, getAdjRibOutYII(entryDep, entryInfo.getRootPath(),
                        routeId), effAttrib,
                        createValue(routeId, path), entryDep.getRibSupport(), tx);
            }
        }
    }

    private void removePathFromDataStore(final RouteEntryDependenciesContainer entryDep,
            final NodeIdentifierWithPredicates routeIdPA, final DOMDataWriteTransaction tx) {
        LOG.trace("Best Path removed {}", this.removedBestPath);
        final YangInstanceIdentifier locRibTarget = entryDep.getLocRibTableTarget();
        final RIBSupport ribSup = entryDep.getRibSupport();
        final PathArgument routeIdAddPath = ribSup.getRouteIdAddPath(this.removedBestPath.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdPA);
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath != null) {
            pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget, null, tx);
        fillAdjRibsOut(null, null, routeIdPA, this.removedBestPath.getPeerId(), entryDep, tx);
    }

    private void addPathToDataStore(final RouteEntryDependenciesContainer entryDep,
            final NodeIdentifierWithPredicates routeIdPA, final DOMDataWriteTransaction tx) {
        final RIBSupport ribSup = entryDep.getRibSupport();
        final YangInstanceIdentifier locRibTarget = entryDep.getLocRibTableTarget();
        final NodeIdentifierWithPredicates routeIdAddPath = ribSup
                .getRouteIdAddPath(this.bestPath.getPathId(), routeIdPA);
        final YangInstanceIdentifier pathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdPA);
        final NormalizedNode<?, ?> value = createValue(routeIdPA, this.bestPath);
        NormalizedNode<?, ?> addPathValue = null;
        YangInstanceIdentifier pathAddPathTarget = null;
        if (routeIdAddPath == null) {
            LOG.trace("Selected best value {}", value);
        } else {
            pathAddPathTarget = ribSup.routePath(locRibTarget.node(ROUTES_IDENTIFIER), routeIdAddPath);
            addPathValue = createValue(routeIdAddPath, this.bestPath);
            LOG.trace("Selected best value {}", addPathValue);
        }
        fillLocRib(pathAddPathTarget == null ? pathTarget : pathAddPathTarget,
                addPathValue == null ? value : addPathValue, tx);
        fillAdjRibsOut(this.bestPath.getAttributes(), value, routeIdPA, this.bestPath.getPeerId(), entryDep, tx);
    }

    final OffsetMap getOffsets() {
        return this.offsets;
    }

    @VisibleForTesting
    private void fillAdjRibsOut(final ContainerNode attributes, final NormalizedNode<?, ?> value,
            final NodeIdentifierWithPredicates routeIdPA, final PeerId fromPeerId,
            final RouteEntryDependenciesContainer routeEntryDep,
            final DOMDataWriteTransaction tx) {
        /*
         * We need to keep track of routers and populate adj-ribs-out, too. If we do not, we need to
         * expose from which client a particular route was learned from in the local RIB, and have
         * the listener perform filtering.
         *
         * We walk the policy set in order to minimize the amount of work we do for multiple peers:
         * if we have two eBGP peers, for example, there is no reason why we should perform the translation
         * multiple times.
         */
        final RIBSupport ribSup = routeEntryDep.getRibSupport();
        final TablesKey localTK = routeEntryDep.getLocalTablesKey();
        final PeerRole fromPeerRole = this.peerTracker.getRole(fromPeerId);
        final BGPRibRoutingPolicy routingPolicies = routeEntryDep.getRoutingPolicies();
        this.peerTracker.getRoles().forEach((key, value1) -> value1.stream()
                .filter(toPeer -> filterRoutes(fromPeerId, toPeer, localTK))
                .forEach(toPeer -> {
                    final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(routeIdPA,
                            fromPeerId, fromPeerRole, toPeer, key);
                    final Optional<ContainerNode> effAttrib
                            = routingPolicies.applyExportPolicies(routeEntry, attributes);
                    update(toPeer, getAdjRibOutYII(routeEntryDep, this.peerTracker.getPeerRibInstanceIdentifier(toPeer),
                            routeIdPA), effAttrib, value, ribSup, tx);
                }));
    }
}