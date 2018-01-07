/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.OffsetMap;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.protocol.bgp.mode.spi.RouteEntryUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ComplexRouteEntry extends AbstractAllPathsRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final Long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(key);
        this.values = map.removeValue(this.values, offset);
        return removeRoute(key, offset);
    }

    @Override
    public MapEntryNode createValue(final NodeIdentifierWithPredicates routeId, final AddPathBestPath path) {
        final OffsetMap map = getOffsets();
        final MapEntryNode mapValues = map.getValue(this.values, map.offsetOf(path.getRouteKey()));
        return RouteEntryUtil.createComplexRouteValue(routeId, path, mapValues);
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId,
            final NodeIdentifier attII,
            final NormalizedNode<?, ?> data) {
        final OffsetMap oldMap = getOffsets();
        final int offset = addRoute(new RouteKey(routerId, remotePathId), attII, data);
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, data);
        return offset;
    }
}
