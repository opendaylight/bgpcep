/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;

final class BestPath {
    private final UnsignedInteger routerId;
    private final BestPathState state;

    BestPath(@Nonnull final UnsignedInteger routerId, @Nonnull final BestPathState state) {
        this.routerId = Preconditions.checkNotNull(routerId);
        this.state = Preconditions.checkNotNull(state);
    }

    UnsignedInteger getRouterId() {
        return this.routerId;
    }

    BestPathState getState() {
        return this.state;
    }

    private ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("routerId", this.routerId);
        toStringHelper.add("state", this.state);
        return toStringHelper;
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.routerId.hashCode();
        result = prime * result + this.state.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BestPath)) {
            return false;
        }
        final BestPath other = (BestPath) obj;
        if (!this.routerId.equals(other.routerId)) {
            return false;
        }
        if (!this.state.equals(other.state)) {
            return false;
        }
        return true;
    }
}
