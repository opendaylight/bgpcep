/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
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
abstract class AbstractRouteEntry {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRouteEntry.class);

    private OffsetMap offsets = OffsetMap.EMPTY;
    private ContainerNode[] values = new ContainerNode[0];
    private Long[] pathsId = new Long[0];
    private UnsignedInteger[] routersId = new UnsignedInteger[0];
    private String[] prefixes = new String[0];
    private List<BestPath> bestPath = new ArrayList<>();
    private ArrayList<BestPath> bestPathRemoved = new ArrayList<>();
    private Long pathIdCounter = Long.valueOf(0);

    private int addRoute(final String key, final UnsignedInteger routerId, final ContainerNode attributes, final String prefix) {
        this.pathIdCounter += 1;
        int offset = this.offsets.offsetOf(key);
        if (offset < 0) {
            final OffsetMap newOffsets = this.offsets.with(key);
            offset = newOffsets.offsetOf(key);

            final ContainerNode[] newAttributes = newOffsets.expand(this.offsets, this.values, offset);
            final Long[] newPathsId = newOffsets.expand(this.offsets, this.pathsId, offset);
            final UnsignedInteger[] newRoutersId = newOffsets.expand(this.offsets, this.routersId, offset);
            final String[] newPrefixes = newOffsets.expand(this.offsets, this.prefixes, offset);
            this.values = newAttributes;
            this.offsets = newOffsets;
            this.pathsId = newPathsId;
            this.routersId = newRoutersId;
            this.prefixes = newPrefixes;
        }

        this.offsets.setValue(this.values, offset, attributes);
        this.offsets.setValue(this.pathsId, offset, this.pathIdCounter);
        this.offsets.setValue(this.routersId, offset, routerId);
        this.offsets.setValue(this.prefixes, offset, prefix);

        LOG.trace("Added route from {} attributes {}", routerId, attributes);
        return offset;
    }

    protected int addRoute(final UnsignedInteger routerId, final String prefix, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        LOG.trace("Find {} in {}", attributesIdentifier, data);
        final ContainerNode advertisedAttrs = (ContainerNode) NormalizedNodes.findNode(data, attributesIdentifier).orNull();
        String key = generateKey(routerId.toString(), prefix);
        return addRoute(key, routerId, advertisedAttrs, prefix);
    }

    final String generateKey(final String routerId, final String prefix) {
        return routerId + prefix;
    }

    /**
     * Remove route
     *
     * @param key
     * @param offset
     * @return true if it was the last route
     */
    protected final boolean removeRoute(final String key, final int offset) {
        this.values = this.offsets.removeValue(this.values, offset);
        this.routersId = this.offsets.removeValue(this.routersId, offset);
        this.pathsId = this.offsets.removeValue(this.pathsId, offset);
        this.prefixes = this.offsets.removeValue(this.prefixes, offset);
        this.offsets = this.offsets.without(key);
        if (this.offsets.size() == 0) {
            return true;
        }
        return false;
    }

    final boolean selectBest(final long localAs, final long nBestPaths) {
        List<BestPath> newBestPathList = new ArrayList<>();
        final List<String> keyList = this.offsets.getRouteKeysList();
        final long maxSearch = nBestPaths < this.offsets.size() && nBestPaths != 0 ? nBestPaths : this.offsets.size();
        for (long i = 0; i < maxSearch; ++i) {
            final BestPath newBest = selectBest(localAs, keyList);
            newBestPathList.add(newBest);
            keyList.remove(newBest.getKey());
        }

        this.bestPathRemoved = new ArrayList<>(this.bestPath);
        if (this.bestPathRemoved.removeAll(newBestPathList) || !this.bestPath.equals(newBestPathList)) {
            this.bestPath = newBestPathList;
            LOG.trace("Actual Best {}, removed best {}", this.bestPath, this.bestPathRemoved);
            return true;
        }
        return false;
    }

    /**
     * Process best path selection
     *
     * @param localAs
     * @param keyList
     * @return the best path inside offset map passed
     */
    final private BestPath selectBest(final long localAs, final List<String> keyList) {
        /*
         * FIXME: optimize flaps by making sure we consider stability of currently-selected route.
         */
        final BestPathSelector selector = new BestPathSelector(localAs);
        keyList.forEach(key -> selectBest(key, selector));
        LOG.trace("Best path selected {}", this.bestPath);
        return selector.result();
    }

    final private void selectBest(final String key, final BestPathSelector selector) {
        final int offset = this.offsets.offsetOf(key);
        final UnsignedInteger routerId = this.offsets.getValue(this.routersId, offset);
        final ContainerNode attributes = this.offsets.getValue(this.values, offset);
        final Long pathId = this.offsets.getValue(this.pathsId, offset);
        final String prefix = this.offsets.getValue(this.prefixes, offset);
        LOG.trace("Processing router key {} attributes {}", key, attributes);
        selector.processPath(routerId, attributes, key, offset, pathId, prefix);
    }

    protected final OffsetMap getOffsets() {
        return this.offsets;
    }

    protected List<BestPath> getBestPathRemovedList() {
        return Collections.unmodifiableList(this.bestPathRemoved);
    }

    protected List<BestPath> getBestPathList() {
        return Collections.unmodifiableList(this.bestPath);
    }

    protected final int size() {
        return this.offsets.size();
    }

    abstract boolean removeRoute(final UnsignedInteger routerId, final String prefix);

    abstract MapEntryNode createValue(final YangInstanceIdentifier.PathArgument routeId, final BestPath path);
}
