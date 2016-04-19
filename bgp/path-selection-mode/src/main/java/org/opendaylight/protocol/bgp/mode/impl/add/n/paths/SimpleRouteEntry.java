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
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

final class SimpleRouteEntry extends AbstractNPathsRouteEntry {
    public SimpleRouteEntry(final Long nBestPaths) {
        super(nBestPaths);
    }

    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final Long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        return removeRoute(key, getOffsets().offsetOf(key));
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = Builders.mapEntryBuilder();
        b.withNodeIdentifier((NodeIdentifierWithPredicates) routeId);
        b.addChild(path.getAttributes());
        return b.build();
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data) {
        return addRoute(new RouteKey(routerId, remotePathId), attributesIdentifier, data);
    }
}