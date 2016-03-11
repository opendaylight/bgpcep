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
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.ExportPolicyPeerTracker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class BaseComplexRouteEntry extends BaseAbstractRouteEntry {
    private static final MapEntryNode[] EMPTY_VALUES = new MapEntryNode[0];
    private MapEntryNode[] values = EMPTY_VALUES;

    public BaseComplexRouteEntry(final PathArgument routeId, final YangInstanceIdentifier routesYangId, final RIBSupport ribSupport,
        final ExportPolicyPeerTracker exportPPT, final TablesKey localTK, final CacheDisconnectedPeers cacheDP) {
        super(routeId, routesYangId, ribSupport, exportPPT, localTK, cacheDP);
    }

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
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(path.getRouterId());
        return map.getValue(this.values, offset);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId) {
        final OffsetMap map = getOffsets();
        final int offset = map.offsetOf(routerId);
        final boolean ret = removeRoute(routerId, offset);
        this.values = map.removeValue(this.values, offset);
        return ret;
    }
}
