/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;

public class AddPathBestNPathSelection implements PathSelectionMode {
    private final Long npaths;
    private final BGPPeerTracker peerTracker;

    public AddPathBestNPathSelection(final Long npaths, final BGPPeerTracker peerTracker) {
        this.npaths = npaths;
        this.peerTracker = requireNonNull(peerTracker);
    }

    @Override
    public void close() throws Exception {
        //no-op
    }

    @Override
    public RouteEntry createRouteEntry(final boolean isComplex) {
        return isComplex ? new ComplexRouteEntry(this.getNBestPaths(), this.peerTracker)
                : new SimpleRouteEntry(this.getNBestPaths(), this.peerTracker);
    }

    public Long getNBestPaths() {
        return this.npaths;
    }
}