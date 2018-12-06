/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedInteger;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Immutable;

@NonNullByDefault
final class RouteKey implements Comparable<RouteKey>, Immutable {
    private final UnsignedInteger routerId;
    private final long remotePathId;

    RouteKey(final UnsignedInteger routerId, final Long remotePathId) {
        this.routerId = requireNonNull(routerId);
        this.remotePathId = remotePathId.longValue();
    }

    UnsignedInteger getRouterId() {
        return routerId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Long.hashCode(remotePathId);
        result = prime * result + routerId.hashCode();
        return result;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RouteKey)) {
            return false;
        }

        final RouteKey other = (RouteKey) obj;
        return remotePathId == other.remotePathId && routerId.equals(other.routerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("externalPathId", remotePathId)
                .add("routerId", routerId)
                .toString();
    }

    @Override
    public int compareTo(final RouteKey otherRouteKey) {
        int cmp;
        return (cmp = routerId.compareTo(otherRouteKey.routerId)) != 0 ? cmp
                : Long.compare(remotePathId, otherRouteKey.remotePathId);
    }
}
