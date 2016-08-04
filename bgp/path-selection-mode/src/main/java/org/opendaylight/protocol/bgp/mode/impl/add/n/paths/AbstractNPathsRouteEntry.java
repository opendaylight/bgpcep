/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathAbstractRouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractNPathsRouteEntry extends AddPathAbstractRouteEntry {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNPathsRouteEntry.class);

    private final long nBestPaths;

    AbstractNPathsRouteEntry(final Long nBestPaths) {
        this.nBestPaths = nBestPaths;
    }

    @Override
    public final boolean selectBest(final long localAs) {
        final List<AddPathBestPath> newBestPathList = new ArrayList<>();
        final List<RouteKey> keyList = this.offsets.getRouteKeysList();
        final long maxSearch = this.nBestPaths < this.offsets.size() && this.nBestPaths != 0 ? this.nBestPaths : this.offsets.size();
        for (long i = 0; i < maxSearch; ++i) {
            final AddPathBestPath newBest = selectBest(localAs, keyList);
            newBestPathList.add(newBest);
            keyList.remove(newBest.getRouteKey());
        }

        if(this.bestPath != null) {
            this.bestPathRemoved = new ArrayList<>(this.bestPath);
        }
        if (!newBestPathList.equals(this.bestPath) ||
            this.bestPathRemoved != null && this.bestPathRemoved.removeAll(newBestPathList) && !this.bestPathRemoved.isEmpty()) {
            this.bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", this.bestPath, this.bestPathRemoved);
            return true;
        }
        return false;
    }
}
