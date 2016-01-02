/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ComplexRouteEntry extends AbstractRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    @Override
    public int addRoute(final UnsignedInteger routerId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        final OffsetMap oldMap = getOffsets();
        final int offset = super.addRoute(routerId, attributesIdentifier, data);
        final OffsetMap newMap = getOffsets();

        if (!newMap.equals(oldMap)) {
            this.values = newMap.expand(oldMap, this.values, offset);
        }

        newMap.setValue(this.values, offset, data);
        return offset;
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(getBestRouterId());
        return map.getValue(this.values, offset);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(routerId);

        final boolean ret = removeRoute(offset);
        // FIXME: actually shrink the array
        map.setValue(this.values, offset, null);
        return ret;
    }
}
