/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;

/**
 * Combined key formed as a concatenation of source peer and route identifiers.
 * This is used to internally track updates which need to be processed.
 */
final class RouteUpdateKey {
    private final @NonNull RouterId peerId;
    private final @NonNull String routeId;

    RouteUpdateKey(final RouterId peerId, final String routeKey) {
        this.peerId = requireNonNull(peerId);
        routeId = requireNonNull(routeKey);
    }

    @NonNull RouterId getPeerId() {
        return peerId;
    }

    @NonNull String getRouteId() {
        return routeId;
    }

    @Override
    public int hashCode() {
        return 31 * 31 + 31 * peerId.hashCode() + routeId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof RouteUpdateKey other
            && peerId.equals(other.peerId) && routeId.equals(other.routeId);
    }
}
