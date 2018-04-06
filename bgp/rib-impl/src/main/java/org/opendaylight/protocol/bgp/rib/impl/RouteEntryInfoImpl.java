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

public final class RouteEntryInfoImpl implements RouteEntryInfo {
    private final Peer peer;
    private final String key;

    public RouteEntryInfoImpl(final Peer peer, final String key) {
        this.peer = requireNonNull(peer);
        this.key = requireNonNull(key);
    }

    @Override
    public Peer getToPeer() {
        return this.peer;
    }

    @Override
    public String getRouteKey() {
        return this.key;
    }
}
