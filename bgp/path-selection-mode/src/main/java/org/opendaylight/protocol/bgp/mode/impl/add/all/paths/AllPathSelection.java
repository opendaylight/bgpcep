/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;

public class AllPathSelection implements PathSelectionMode {
    private final BGPPeerTracker peerTracker;

    public AllPathSelection(final BGPPeerTracker peerTracker) {
        this.peerTracker = requireNonNull(peerTracker);
    }

    @Override
    public RouteEntry createRouteEntry(final boolean isComplexRoute) {
        return isComplexRoute ? new ComplexRouteEntry(this.peerTracker) : new SimpleRouteEntry(this.peerTracker);
    }

    @Override
    public void close() throws Exception {
        //no-op
    }
}
