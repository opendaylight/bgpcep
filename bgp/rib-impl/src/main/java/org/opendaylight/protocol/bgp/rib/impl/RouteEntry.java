/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import java.util.Objects;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A single route entry inside a route table. Maintains the attributes of
 * from all contributing peers. The information is stored in arrays with a
 * shared map of offsets for peers to allow lookups. This is needed to
 * maintain low memory overhead in face of large number of routes and peers,
 * where individual object overhead becomes the dominating factor.
 */
@NotThreadSafe
final class RouteEntry {
    private static final ContainerNode[] EMPTY_ATTRIBUTES = new ContainerNode[0];

    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
    private BestPath bestPath;

    void addRoute(final UnsignedInteger routerId, final ContainerNode attributes) {
        int offset = offsets.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newOffsets = offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            final ContainerNode[] newAttributes = newOffsets.expand(offsets, this.values, offset);
            this.values = newAttributes;
            this.offsets = newOffsets;
        }

        offsets.setValue(this.values, offset, attributes);
    }

    // Indicates whether this was the last route
    boolean removeRoute(final UnsignedInteger routerId) {
        if (offsets.size() != 1) {
            // FIXME: actually shrink the array
            int offset = offsets.offsetOf(routerId);
            offsets.setValue(values, offset, null);
            return false;
        } else {
            return true;
        }
    }

    // Indicates whether best has changed
    boolean selectBest(final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BestPathSelector selector = new BestPathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < this.offsets.size(); ++i) {
            final UnsignedInteger routerId = this.offsets.getRouterId(i);
            final ContainerNode attributes = this.offsets.getValue(this.values, i);

            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final BestPath newBestPath = selector.result();
        // FIXME: run deeper comparison
        final boolean ret = !Objects.equals(bestPath, newBestPath);

        bestPath = newBestPath;
        return ret;
    }

    NormalizedNode<?, ?> bestValue(final PathArgument key) {
        // TODO Auto-generated method stub
        return null;
    }
}
