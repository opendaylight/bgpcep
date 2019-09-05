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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPath;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class AddPathBestPath extends AbstractBestPath {
    private final @NonNull RouteKey routeKey;
    private final @NonNull Uint32 pathId;
    private final int offset;

    AddPathBestPath(final @NonNull BestPathState state, final @NonNull RouteKey key, final @NonNull Uint32 pathId,
            final int offset) {
        super(state);
        this.routeKey = requireNonNull(key);
        this.pathId = requireNonNull(pathId);
        this.offset = offset;
    }

    RouteKey getRouteKey() {
        return this.routeKey;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper.add("routeKey", this.routeKey)
            .add("pathId", this.pathId)
            .add("offset", offset));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.routeKey.hashCode();
        result = prime * result + this.state.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AddPathBestPath)) {
            return false;
        }
        final AddPathBestPath other = (AddPathBestPath) obj;
        if (!this.routeKey.equals(other.routeKey)) {
            return false;
        }
        /*
        We don't check offset position since it will change as new path is added, and we want to be able
        to use List comparison between old bestpath list and new best path list.
        */
        if (!this.state.equals(other.state)) {
            return false;
        }

        return this.pathId.equals(other.pathId);
    }

    @Override
    public RouterId getRouterId() {
        return this.routeKey.getRouterId();
    }

    @Override
    public long getPathId() {
        // FIXME: this should return int bits...
        return this.pathId.longValue();
    }

    /**
     * Return the boxed value equivalent of {@link #getPathId()}. This class uses boxed instances internally, hence
     * this method exposes it.
     *
     * @return Path Id as a {@link Long}
     */
    @NonNull Uint32 getPathIdLong() {
        return this.pathId;
    }
}
