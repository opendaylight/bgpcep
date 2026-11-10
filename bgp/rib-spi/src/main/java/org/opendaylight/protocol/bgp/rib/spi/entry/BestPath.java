/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * A best path selected for a {@link RouteEntry}.
 *
 * @since 3.0.0
 */
@NonNullByDefault
public interface BestPath {
    /**
     * {@return the {@link RouterId}}
     */
    RouterId routerId();

    /**
     * {@return {@link #routerId()} converted to a {@link PeerId}}
     */
    default PeerId peerId() {
        return routerId().getPeerId();
    }

    /**
     * {@return the path attributes}
     */
    ContainerNode attributes();

    /**
     * {@return the raw path id}
     *
     * @see Uint32#fromIntBits(int)
     */
    int pathIdBits();

    /**
     * {@return {@code true} if this path is depreferenced, {@code false} otherwise}
     */
    boolean depreferenced();

    /**
     * {@return {@link ActualBestPathRoutes} of this path}
     *
     * @param ribSupport RIB Support
     * @param route the route
     */
    default ActualBestPathRoutes toActual(final RIBSupport ribSupport, final MapEntryNode route) {
        return new ActualBestPathRoutes(ribSupport, route, this);
    }

    /**
     * {@return {@link AdvertizedRoute} of this path}
     *
     * @param ribSupport RIB Support
     * @param route the value of {@link AdvertizedRoute#getRoute()}
     * @param isFirstBestPath the value of {@link AdvertizedRoute#isFirstBestPath()}
     */
    default AdvertizedRoute toAdvertized(final RIBSupport ribSupport, final MapEntryNode route,
            final boolean isFirstBestPath) {
        return new AdvertizedRoute(ribSupport, route, this, isFirstBestPath);
    }
}
