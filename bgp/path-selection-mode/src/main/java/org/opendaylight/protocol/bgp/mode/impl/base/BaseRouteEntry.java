/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BaseRouteEntry<C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>, R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> implements RouteEntry<C, S, R, I> {
    private static final class Stale<C extends Routes & DataObject & ChoiceIn<Tables>,
            S extends ChildOf<? super C>, R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> extends StaleBestPathRoute<C, S, R, I> {
        Stale(final I nonAddPathRouteKeyIdentifier) {
            super(nonAddPathRouteKeyIdentifier);
        }

        @Override
        public List<I> getStaleRouteKeyIdentifiers() {
            return Collections.singletonList(getNonAddPathRouteKeyIdentifier());
        }

        @Override
        public List<I> getAddPathRouteKeyIdentifiers() {
            return Collections.emptyList();
        }

        @Override
        public boolean isNonAddPathBestPathNew() {
            return true;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BaseRouteEntry.class);
    private static final Route[] EMPTY_VALUES = new Route[0];

    private RouterIdOffsets offsets = RouterIdOffsets.EMPTY;
    private R[] values = (R[]) EMPTY_VALUES;
    private BaseBestPath bestPath;
    private BaseBestPath removedBestPath;

    BaseRouteEntry() {
    }

    @Override
    public boolean removeRoute(final RouterId routerId, final Uint32 remotePathId) {
        final int offset = this.offsets.offsetOf(routerId);
        this.values = this.offsets.removeValue(this.values, offset, (R[]) EMPTY_VALUES);
        this.offsets = this.offsets.without(routerId);
        return this.offsets.isEmpty();
    }

    private R createRoute(final RIBSupport<C, S, R, I> ribSup, final String routeKey) {
        final I key = ribSup.createRouteListKey(routeKey);
        final R route = this.offsets.getValue(this.values, this.offsets.offsetOf(bestPath.getRouterId()));
        return ribSup.createRoute(route, key, bestPath.getAttributes());
    }

    @Override
    public boolean selectBest(final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BasePathSelector selector = new BasePathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < this.offsets.size(); ++i) {
            final RouterId routerId = this.offsets.getKey(i);
            final Attributes attributes = this.offsets.getValue(this.values, i).getAttributes();
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
    public int addRoute(final RouterId routerId, final Uint32 remotePathId, final R route) {
        int offset = this.offsets.offsetOf(routerId);
        if (offset < 0) {
            final RouterIdOffsets newOffsets = this.offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            this.values = newOffsets.expand(this.offsets, this.values, offset);
            this.offsets = newOffsets;
        }

        this.offsets.setValue(this.values, offset, route);
        LOG.trace("Added route {} from {}", route, routerId);
        return offset;
    }

    @Override
    public Optional<StaleBestPathRoute<C, S, R, I>> removeStalePaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
        if (removedBestPath == null) {
            return Optional.empty();
        }
        removedBestPath = null;
        return Optional.of(new Stale<C, S, R, I>(ribSupport.createRouteListKey(routeKey)));
    }

    @Override
    public List<AdvertizedRoute<C, S, R, I>> newBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final String routeKey) {
        if (this.bestPath == null) {
            return Collections.emptyList();
        }
        final R route = createRoute(ribSupport, routeKey);
        final AdvertizedRoute<C, S, R, I> adv = new AdvertizedRoute<>(ribSupport, route, this.bestPath.getAttributes(),
                this.bestPath.getPeerId(), this.bestPath.isDepreferenced());
        LOG.trace("Selected best route {}", route);
        return Collections.singletonList(adv);
    }

    @Override
    public List<ActualBestPathRoutes<C, S, R, I>> actualBestPaths(final RIBSupport<C, S, R, I> ribSupport,
            final RouteEntryInfo entryInfo) {
        if (this.bestPath == null) {
            return Collections.emptyList();
        }
        final R route = createRoute(ribSupport, entryInfo.getRouteKey());
        return Collections.singletonList(new ActualBestPathRoutes<>(ribSupport, route, this.bestPath.getPeerId(),
                this.bestPath.getAttributes(), this.bestPath.isDepreferenced()));
    }
}