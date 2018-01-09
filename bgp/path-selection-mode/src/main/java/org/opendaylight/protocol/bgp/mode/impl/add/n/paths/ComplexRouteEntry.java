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
import org.opendaylight.protocol.bgp.mode.impl.add.AddPathBestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.OffsetMap;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.protocol.bgp.mode.spi.RouteEntryUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ComplexRouteEntry extends AbstractNPathsRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    ComplexRouteEntry(final Long npaths) {
        super(npaths);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final Long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(key);
        this.values = map.removeValue(this.values, offset);
        return removeRoute(key, offset);
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final OffsetMap map = getOffsets();
        final MapEntryNode mapValues = map.getValue(this.values, map.offsetOf(((AddPathBestPath) path).getRouteKey()));
        return RouteEntryUtil.createComplexRouteValue(routeId, path, mapValues);
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId,
            final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        final OffsetMap oldMap = getOffsets();
        final int offset = addRoute(new RouteKey(routerId, remotePathId), attributesIdentifier, data);
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, data);
        return offset;
    }
}