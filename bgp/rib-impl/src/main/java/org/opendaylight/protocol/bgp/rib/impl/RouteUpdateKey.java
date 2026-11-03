/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Combined key formed as a concatenation of source peer and route identifiers. This is used to internally track updates
 * which need to be processed.
 */
@NonNullByDefault
record RouteUpdateKey(RouterId peerId, String routeKey) implements Immutable {
    RouteUpdateKey {
        requireNonNull(peerId);
        requireNonNull(routeKey);
    }

    @Override
    public int hashCode() {
        return 31 * peerId.hashCode() + routeKey.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof RouteUpdateKey other
            && peerId.equals(other.peerId) && routeKey.equals(other.routeKey);
    }
}
