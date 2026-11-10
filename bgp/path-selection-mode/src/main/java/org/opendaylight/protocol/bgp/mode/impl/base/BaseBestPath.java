/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPath;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;

@NonNullByDefault
final class BaseBestPath extends AbstractBestPath {
    private final RouterId routerId;

    BaseBestPath(final RouterId routerId, final BestPathState state) {
        super(state);
        this.routerId = requireNonNull(routerId);
    }

    @Override
    public RouterId routerId() {
        return routerId;
    }

    @Override
    public int pathIdBits() {
        // PathIdUtil.NON_PATH_ID_VALUE
        return 0;
    }

    @Override
    public int hashCode() {
        return 31 * routerId.hashCode() + state.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof BaseBestPath other
            && routerId.equals(other.routerId) && state.equals(other.state);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper.add("routerId", routerId));
    }
}
