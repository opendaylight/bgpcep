/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.rib.spi.RouterId;

/**
 * Combined key formed as a concatenation of source peer and route identifiers.
 * This is used to internally track updates which need to be processed.
 */
final class RouteUpdateKey {
    private final RouterId peerId;
    private final String routeId;

    RouteUpdateKey(final RouterId peerId, final String routeKey) {
        this.peerId = requireNonNull(peerId);
        this.routeId = requireNonNull(routeKey);
    }

    RouterId getPeerId() {
        return this.peerId;
    }

    String getRouteId() {
        return this.routeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.peerId.hashCode();
        result = prime * result + this.routeId.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RouteUpdateKey)) {
            return false;
        }
        RouteUpdateKey other = (RouteUpdateKey) obj;
        return peerId.equals(other.peerId) && routeId.equals(other.routeId);
    }
}
