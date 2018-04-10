/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;

final class ComplexRouteEntry extends BaseAbstractRouteEntry {
    private Route[] values = EMPTY_VALUES;

    ComplexRouteEntry(final BGPPeerTracker peerTracker) {
        super(peerTracker);
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final long remotePathId, final Route route) {
        final OffsetMap oldMap = getOffsets();
        final int offset = super.addRoute(routerId, remotePathId, route);
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, route);
        return offset;
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(routerId);
        this.values = map.removeValue(this.values, offset, EMPTY_VALUES);
        return removeRoute(routerId, offset);
    }

    @Override
    public Route createRoute(final RIBSupport ribSup, String routeKey, final long pathId,
            final BaseBestPath path) {
        final OffsetMap map = getOffsets();
        final Route route = map.getValue(this.values, map.offsetOf(path.getRouterId()));
        return ribSup.createRoute(route, routeKey, pathId, path.getAttributes());
    }
}
