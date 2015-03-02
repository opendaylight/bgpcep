/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

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
        return routerId;
    }

    BestPathState getState() {
        return state;
    }
}
