/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.impl.add.all.paths;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

final class SimpleRouteEntry extends AbstractAllPathsRouteEntry {
    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        return removeRoute(key, getOffsets().offsetOf(key));
    }

    @Override
    public MapEntryNode createValue(final YangInstanceIdentifier.PathArgument routeId, final BestPath path) {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> b = Builders.mapEntryBuilder();
        b.withNodeIdentifier((YangInstanceIdentifier.NodeIdentifierWithPredicates) routeId);
        b.addChild(path.getAttributes());
        return b.build();
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attII,
        final NormalizedNode<?, ?> data) {
        return addRoute(new RouteKey(routerId, remotePathId), attII, data);
    }
}
