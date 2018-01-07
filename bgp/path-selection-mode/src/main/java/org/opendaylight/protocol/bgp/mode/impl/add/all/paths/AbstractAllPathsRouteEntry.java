/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathAbstractRouteEntry;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

abstract class AbstractAllPathsRouteEntry extends AddPathAbstractRouteEntry {
    @Override
    public final boolean selectBest(final long localAs) {
        final List<AddPathBestPath> newBestPathList = new ArrayList<>();
        final List<RouteKey> keyList = this.offsets.getRouteKeysList();

        if (!keyList.isEmpty()) {
            /* we set the best path first on List for not supported Add path cases*/
            final AddPathBestPath newBest = selectBest(localAs, keyList);
            newBestPathList.add(newBest);
            keyList.remove(newBest.getRouteKey());
            /*we add the rest of path, regardless in what order they are, since this is all path case */
            for (final RouteKey key : keyList) {
                final int offset = this.offsets.offsetOf(key);
                final ContainerNode attributes = this.offsets.getValue(this.values, offset);
                requireNonNull(key.getRouteId(), "Router ID may not be null");
                if (attributes != null) {
                    final BestPathState state = new BestPathStateImpl(attributes);
                    final AddPathBestPath bestPath = new AddPathBestPath(state, key, offset,
                            this.offsets.getValue(this.pathsId, offset));
                    newBestPathList.add(bestPath);
                }
            }
        }
        return isBestPathNew(ImmutableList.copyOf(newBestPathList));
    }
}
