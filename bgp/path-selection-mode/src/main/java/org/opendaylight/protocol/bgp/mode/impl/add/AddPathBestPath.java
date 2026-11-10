/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPath;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.yang.common.Uint32;

@NonNullByDefault
final class AddPathBestPath extends AbstractBestPath {
    private final RouteKey routeKey;
    private final Uint32 pathId;
    // Not part of hashCode/equals since it will change as new path is added, and we want to be able to use List
    // comparison between old best path list and new best path list.
    private final int offset;

    AddPathBestPath(final BestPathState state, final RouteKey routeKey, final Uint32 pathId, final int offset) {
        super(state);
        this.routeKey = requireNonNull(routeKey);
        this.pathId = requireNonNull(pathId);
        this.offset = offset;
    }

    RouteKey getRouteKey() {
        return routeKey;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public RouterId routerId() {
        return routeKey.routerId();
    }

    @Override
    public int pathIdBits() {
        return pathId.intValue();
    }

    /**
     * Return the boxed value equivalent of {@link #pathIdBits()}. This class uses boxed instances internally, hence
     * this method exposes it.
     *
     * @return Path Id as a {@link Long}
     */
    Uint32 getPathIdLong() {
        return pathId;
    }

    @Override
    public int hashCode() {
        return 31 * routeKey.hashCode() + state.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof AddPathBestPath other
            && routeKey.equals(other.routeKey) && state.equals(other.state) && pathId.equals(other.pathId);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper.add("routeKey", routeKey)
            .add("pathId", pathId)
            .add("offset", offset));
    }
}
