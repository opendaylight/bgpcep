/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.spi.RouteEntryUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class BaseComplexRouteEntry extends BaseAbstractRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attrII,
            final NormalizedNode<?, ?> data) {
        final OffsetMap oldMap = getOffsets();
        final int offset = super.addRoute(routerId, remotePathId, attrII, data);
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, data);
        return offset;
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final Long remotePathId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(routerId);
        this.values = map.removeValue(this.values, offset);
        return removeRoute(routerId, offset);
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final OffsetMap map = getOffsets();
        final MapEntryNode mapValues = map.getValue(this.values, map.offsetOf(path.getRouterId()));
        return RouteEntryUtil.createComplexRouteValue(routeId, path, mapValues);
    }
}
