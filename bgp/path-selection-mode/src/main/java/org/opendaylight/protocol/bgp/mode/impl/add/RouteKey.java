/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;

public final class RouteKey implements Comparable<RouteKey> {
    private final Long remotePathId;
    private final UnsignedInteger routerId;

    public RouteKey(final UnsignedInteger routerId, final Long remotePathId) {
        this.routerId = routerId;
        this.remotePathId = remotePathId != null ? remotePathId : 0;
    }

    private Long getExternalPathId() {
        return this.remotePathId;
    }

    public UnsignedInteger getRouteId() {
        return this.routerId;
    }

    private MoreObjects.ToStringHelper addToStringAttributes(final MoreObjects.ToStringHelper toStringHelper) {
        toStringHelper.add("externalPathId", this.remotePathId);
        toStringHelper.add("routerId", this.routerId);
        return toStringHelper;
    }

    @Override
    public int hashCode() {
        final int prime = 27;
        int result = 1;
        result = prime * result + this.remotePathId.hashCode();
        result = prime * result + this.routerId.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RouteKey)) {
            return false;
        }

        final RouteKey other = (RouteKey) obj;
        return this.remotePathId.equals(other.remotePathId) && this.routerId.equals(other.routerId);
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public int compareTo(@Nonnull final RouteKey otherRouteKey) {
        final int routeIdCompareTo = this.routerId.compareTo(otherRouteKey.getRouteId());
        return routeIdCompareTo == 0 ? this.remotePathId.compareTo(otherRouteKey.getExternalPathId())
                : routeIdCompareTo;
    }
}
