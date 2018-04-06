/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.spi;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

public abstract class AbstractRouteEntry<T extends BestPath> implements RouteEntry {
    protected final BGPPeerTracker peerTracker;

    public AbstractRouteEntry(final BGPPeerTracker peerTracker) {
        this.peerTracker = requireNonNull(peerTracker);
    }

    /**
     * Create new Route with route jey created from passed parameters.
     */
    public abstract Route createRoute(@Nonnull RIBSupport ribSup, @Nonnull String routeKey, long pathId, T path);

    /**
     * Returns true if route can be send.
     */
    protected boolean filterRoutes(final PeerId fromPeer, final Peer toPeer, final TablesKey localTK) {
        return !(toPeer == null
                || !toPeer.supportsTable(localTK)
                || PeerRole.Internal.equals(toPeer.getRole()))
                && !fromPeer.equals(toPeer.getPeerId());
    }
}
