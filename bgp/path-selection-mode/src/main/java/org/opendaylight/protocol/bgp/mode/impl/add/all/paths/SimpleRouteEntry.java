/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yangtools.yang.binding.Identifier;

final class SimpleRouteEntry extends AbstractAllPathsRouteEntry {
    SimpleRouteEntry(final BGPPeerTracker peerTracker) {
        super(peerTracker);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        return removeRoute(key, getOffsets().offsetOf(key));
    }

    @Override
    public Route createRoute(final RIBSupport ribSup, final Identifier routeKey, final long pathId,
            final AddPathBestPath path) {
        return ribSup.createRoute(null, routeKey, pathId, path.getAttributes());
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final long remotePathId, final Route route) {
        return addRoute(new RouteKey(routerId, remotePathId), route.getAttributes());
    }
}
