/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.n.paths;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.impl.OffsetMap;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ComplexRouteEntry extends AbstractNPathsRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    public ComplexRouteEntry(final Long nBestPaths) {
        super(nBestPaths);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final OffsetMap<RouteKey> map = getOffsets();
        final int offset = map.offsetOf(key);
        final boolean ret = removeRoute(key, offset);
        this.values = map.removeValue(this.values, offset);
        return ret;
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final OffsetMap<RouteKey> map = getOffsets();
        final int offset = map.offsetOf(((AddPathBestPath) path).getRouteKey());
        return map.getValue(this.values, offset);
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        final OffsetMap<RouteKey> oldMap = getOffsets();
        final int offset = addRoute(new RouteKey(routerId, remotePathId), attributesIdentifier, data);
        final OffsetMap<RouteKey> newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, data);
        return offset;
    }
}