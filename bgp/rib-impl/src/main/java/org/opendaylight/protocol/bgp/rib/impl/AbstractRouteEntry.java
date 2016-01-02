/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.protocol.bgp.rib.impl.spi.RouteEntry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single route entry inside a route table. Maintains the attributes of
 * from all contributing peers. The information is stored in arrays with a
 * shared map of offsets for peers to allow lookups. This is needed to
 * maintain low memory overhead in face of large number of routes and peers,
 * where individual object overhead becomes the dominating factor.
 */
@NotThreadSafe
abstract class AbstractRouteEntry implements RouteEntry {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRouteEntry.class);

    private static final ContainerNode[] EMPTY_ATTRIBUTES = new ContainerNode[0];

    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = EMPTY_ATTRIBUTES;
    private BestPath bestPath;

    private int addRoute(final UnsignedInteger routerId, final ContainerNode attributes) {
        int offset = this.offsets.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(routerId);
            offset = newOffsets.offsetOf(routerId);

            final ContainerNode[] newAttributes = newOffsets.expand(this.offsets, this.values, offset);
            this.values = newAttributes;
            this.offsets = newOffsets;
        }

        this.offsets.setValue(this.values, offset, attributes);
        LOG.trace("Added route from {} attributes {}", routerId, attributes);
        return offset;
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orNull();
        return addRoute(routerId, advertisedAttrs);
    }

    @Override
    public final boolean removeRoute(final int offset) {
        if (this.offsets.size() != 1) {
            // FIXME: actually shrink the array
            this.offsets.setValue(this.values, offset, null);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public final boolean selectBest(final long localAs) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BestPathSelector selector = new BestPathSelector(localAs);

        // Select the best route.
        for (int i = 0; i < this.offsets.size(); ++i) {
            final UnsignedInteger routerId = this.offsets.getRouterId(i);
            final ContainerNode attributes = this.offsets.getValue(this.values, i);
            LOG.trace("Processing router id {} attributes {}", routerId, attributes);
            selector.processPath(routerId, attributes);
        }

        // Get the newly-selected best path.
        final BestPath newBestPath = selector.result();
        final boolean ret = !newBestPath.equals(this.bestPath);
        LOG.trace("Previous best {}, current best {}, result {}", this.bestPath, newBestPath, ret);
        this.bestPath = newBestPath;
        return ret;
    }

    @Override
    public final ContainerNode attributes() {
        return this.bestPath.getState().getAttributes();
    }

    protected final OffsetMap getOffsets() {
        return this.offsets;
    }

    protected final UnsignedInteger getBestRouterId() {
        return this.bestPath.getRouterId();
    }
}
