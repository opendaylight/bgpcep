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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.Uint32;

@NonNullByDefault
final class RouteKey implements Comparable<RouteKey>, Immutable {
    private final RouterId routerId;
    private final int remotePathBits;

    RouteKey(final RouterId routerId, final Uint32 remotePathId) {
        this.routerId = requireNonNull(routerId);
        remotePathBits = remotePathId.intValue();
    }

    RouterId routerId() {
        return routerId;
    }

    @Override
    public int hashCode() {
        return 31 * remotePathBits + routerId.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof RouteKey other
            && remotePathBits == other.remotePathBits && routerId.equals(other.routerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("externalPathId", Integer.toUnsignedLong(remotePathBits))
            .add("routerId", routerId)
            .toString();
    }

    @Override
    public int compareTo(final RouteKey other) {
        final int cmp = routerId.compareTo(other.routerId);
        return cmp != 0 ? cmp : Integer.compareUnsigned(remotePathBits, other.remotePathBits);
    }
}
