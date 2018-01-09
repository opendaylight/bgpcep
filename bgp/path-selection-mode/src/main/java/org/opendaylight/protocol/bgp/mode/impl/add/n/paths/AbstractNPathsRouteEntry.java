/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathAbstractRouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;

abstract class AbstractNPathsRouteEntry extends AddPathAbstractRouteEntry {
    private final long nBestPaths;

    AbstractNPathsRouteEntry(final Long nBestPaths) {
        this.nBestPaths = nBestPaths;
    }

    @Override
    public final boolean selectBest(final long localAs) {
        final List<AddPathBestPath> newBestPathList = new ArrayList<>();
        final List<RouteKey> keyList = this.offsets.getRouteKeysList();
        final long maxSearch = this.nBestPaths < this.offsets.size()
                && this.nBestPaths != 0 ? this.nBestPaths : this.offsets.size();
        for (long i = 0; i < maxSearch; ++i) {
            final AddPathBestPath newBest = selectBest(localAs, keyList);
            newBestPathList.add(newBest);
            keyList.remove(newBest.getRouteKey());
        }
        return isBestPathNew(ImmutableList.copyOf(newBestPathList));
    }
}
