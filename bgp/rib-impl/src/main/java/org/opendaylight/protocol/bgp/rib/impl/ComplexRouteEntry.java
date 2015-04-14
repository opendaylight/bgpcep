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
    protected int addRoute(UnsignedInteger routerId, NodeIdentifier attributesIdentifier, NormalizedNode<?, ?> data) {
        final OffsetMap oldMap = getOffsets();
        final int offset = super.addRoute(routerId, attributesIdentifier, data);
        final OffsetMap newMap = getOffsets();
        
        if (!newMap.equals(oldMap)) {
            values = newMap.expand(oldMap, values, offset);
        }
        
        newMap.setValue(values, offset, data);
        return offset;
    }

    @Override
    protected MapEntryNode createValue(final PathArgument routeId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(getBestRouterId());
        return map.getValue(values, offset);
    }

    @Override
    boolean removeRoute(UnsignedInteger routerId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(routerId);
        
        final boolean ret = removeRoute(offset);
        // FIXME: actually shrink the array
        map.setValue(values, offset, null);
        return ret;
    }
}
