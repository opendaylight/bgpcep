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
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPath;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;

public final class AddPathBestPath extends AbstractBestPath {
    private final RouteKey routeKey;
    private final int offsetPosition;
    private final long pathId;

    public AddPathBestPath(@Nonnull final BestPathState state, @Nonnull final RouteKey key, final int offsetPosition, final Long pathId) {
        super(state);
        this.routeKey = requireNonNull(key);
        this.offsetPosition = offsetPosition;
        this.pathId = pathId;
    }

    public RouteKey getRouteKey() {
        return this.routeKey;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("routeKey", this.routeKey);
        toStringHelper.add("state", this.state);
        toStringHelper.add("offsetPosition", this.offsetPosition);
        toStringHelper.add("pathId", this.pathId);
        return toStringHelper;
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
        We don't check offset position since it will change as new path is added, and we want to be able to use List comparison between old
        bestpath list and new best path list.
        */
        if (!this.state.equals(other.state)) {
            return false;
        }

        return this.pathId == other.pathId;
    }

    @Override
    public final UnsignedInteger getRouterId() {
        return this.routeKey.getRouteId();
    }

    @Override
    public final PeerId getPeerId() {
        return RouterIds.createPeerId(this.routeKey.getRouteId());
    }

    @Override
    public long getPathId() {
        return this.pathId;
    }
}
