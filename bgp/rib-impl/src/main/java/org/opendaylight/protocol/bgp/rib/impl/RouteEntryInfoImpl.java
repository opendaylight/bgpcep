/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.yangtools.yang.binding.Identifier;

public final class RouteEntryInfoImpl<N extends Identifier> implements RouteEntryInfo<N> {
    private final Peer peer;
    private final N key;

    public RouteEntryInfoImpl(final Peer peer, final N key) {
        this.peer = requireNonNull(peer);
        this.key = requireNonNull(key);
    }

    @Override
    public Peer getToPeer() {
        return this.peer;
    }

    @Override
    public N getRouteKey() {
        return this.key;
    }
}
