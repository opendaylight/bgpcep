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
import org.opendaylight.yangtools.yang.binding.Identifier;

final class SimpleRouteEntry extends BaseAbstractRouteEntry {
    SimpleRouteEntry(final BGPPeerTracker peerTracker) {
        super(peerTracker);
    }

    @Override
    public Route createRoute(final RIBSupport ribSup, final Identifier routeKey, final long pathId,
            final BaseBestPath path) {
        return ribSup.createRoute(null, routeKey, pathId, path.getAttributes());
    }

    @Override
    public boolean removeRoute(UnsignedInteger routerId, final long remotePathId) {
        return removeRoute(routerId, getOffsets().offsetOf(routerId));
    }
}
