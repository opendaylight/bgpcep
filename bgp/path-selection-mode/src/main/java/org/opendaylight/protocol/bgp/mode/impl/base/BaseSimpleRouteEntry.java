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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

final class BaseSimpleRouteEntry extends BaseAbstractRouteEntry {
    @Override
    public boolean removeRoute(UnsignedInteger routerId, final long remotePathId) {
        return removeRoute(routerId, getOffsets().offsetOf(routerId));
    }

    @Override
    public MapEntryNode createValue(final PathArgument routeId, final BestPath path) {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = Builders.mapEntryBuilder();
        b.withNodeIdentifier((NodeIdentifierWithPredicates) routeId);
        b.addChild(path.getAttributes());
        return b.build();
    }
}
