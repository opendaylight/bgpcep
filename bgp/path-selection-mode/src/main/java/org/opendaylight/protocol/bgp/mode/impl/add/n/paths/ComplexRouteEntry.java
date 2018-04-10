/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.OffsetMap;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;

final class ComplexRouteEntry extends AbstractNPathsRouteEntry {
    private static final Route[] EMPTY_VALUES = new Route[0];
    private Route[] values = EMPTY_VALUES;

    ComplexRouteEntry(final long npaths, final BGPPeerTracker peerTracker) {
        super(npaths, peerTracker);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(key);
        this.values = map.removeValue(this.values, offset);
        return removeRoute(key, offset);
    }

    @Override
    public Route createRoute(final RIBSupport ribSup, final String routeKey, final long pathId,
            final AddPathBestPath path) {
        final OffsetMap map = getOffsets();
        final Route route = map.getValue(this.values, map.offsetOf(path.getRouteKey()));
        return ribSup.createRoute(route, routeKey, pathId, path.getAttributes());
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final long remotePathId, final Route route) {
        final OffsetMap oldMap = getOffsets();
        final int offset = addRoute(new RouteKey(routerId, remotePathId), route.getAttributes());
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, route);
        return offset;
    }
}