/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * Combined key formed as a concatenation of source peer and route identifiers.
 * This is used to internally track updates which need to be processed.
 */
final class RouteUpdateKey {
    private final PeerId peerId;
    private final PathArgument routeId;

    RouteUpdateKey(final PeerId peerId, final PathArgument routeId) {
        this.peerId = requireNonNull(peerId);
        this.routeId = requireNonNull(routeId);
    }

    PeerId getPeerId() {
        return this.peerId;
    }

    PathArgument getRouteId() {
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
        if (!this.peerId.equals(other.peerId)) {
            return false;
        }
        if (!this.routeId.equals(other.routeId)) {
            return false;
        }
        return true;
    }
}
