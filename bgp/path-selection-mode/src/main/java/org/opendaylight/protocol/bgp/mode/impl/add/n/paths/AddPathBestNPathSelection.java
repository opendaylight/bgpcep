/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.api.RouteEntry;

public class AddPathBestNPathSelection implements PathSelectionMode {
    private final Long npaths;

    public AddPathBestNPathSelection(final Long npaths) {
        this.npaths = npaths;
    }

    @Override
    public void close() throws Exception {
        //no-op
    }

    @Override
    public RouteEntry createRouteEntry(final boolean isComplex) {
        return isComplex ? new ComplexRouteEntry(this.getNBestPaths()) : new SimpleRouteEntry(this.getNBestPaths());
    }

    public Long getNBestPaths() {
        return this.npaths;
    }
}