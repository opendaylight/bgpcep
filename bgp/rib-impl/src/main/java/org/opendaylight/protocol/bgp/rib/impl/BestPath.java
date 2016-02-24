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
    private final String key;
    private final BestPathState state;
    private final int offsetPosition;
    private final Long pathId;
    private final String prefix;
    private final UnsignedInteger bestRouterId;

    BestPath(@Nonnull final BestPathState state, @Nonnull final String key, final int offsetPosition, final UnsignedInteger bestRouterId, final Long pathId, final String prefix) {
        this.state = Preconditions.checkNotNull(state);
        this.key = Preconditions.checkNotNull(key);
        this.offsetPosition = offsetPosition;
        this.bestRouterId = bestRouterId;
        this.pathId = pathId;
        this.prefix = prefix;
    }

    String getKey() {
        return this.key;
    }

    BestPathState getState() {
        return this.state;
    }

    int getOfssetPosition() {
        return this.offsetPosition;
    }

    private ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("key", this.key);
        toStringHelper.add("state", this.state);
        toStringHelper.add("offsetPosition", this.offsetPosition);
        toStringHelper.add("pathId", this.pathId);
        toStringHelper.add("prefix", this.prefix);
        toStringHelper.add("bestRouterId", this.bestRouterId);
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
        result = prime * result + this.key.hashCode();
        result = prime * result + this.state.hashCode();
        result = prime * result + this.pathId.hashCode();
        result = prime * result + this.prefix.hashCode();
        result = prime * result + this.bestRouterId.hashCode();
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
        if (!this.key.equals(other.key)) {
            return false;
        }
        if (!this.state.equals(other.state)) {
            return false;
        }
        if (!(this.offsetPosition == other.offsetPosition)) {
            return false;
        }

        if (!this.pathId.equals(other.pathId)) {
            return false;
        }

        if (!this.prefix.equals(other.prefix)) {
            return false;
        }

        if (!this.bestRouterId.equals(other.bestRouterId)) {
            return false;
        }
        return true;
    }

    Long getPathId() {
        return this.pathId;
    }

    String getPrefix() {
        return prefix;
    }

    UnsignedInteger getBestRouterId(){
        return this.bestRouterId;
    }
}
