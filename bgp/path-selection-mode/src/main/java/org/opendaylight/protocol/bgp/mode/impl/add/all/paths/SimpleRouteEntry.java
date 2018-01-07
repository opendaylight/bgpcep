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
import org.opendaylight.protocol.bgp.mode.impl.add.RouteKey;
import org.opendaylight.protocol.bgp.mode.spi.RouteEntryUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class SimpleRouteEntry extends AbstractAllPathsRouteEntry {
    @Override
    public boolean removeRoute(final UnsignedInteger routerId, final Long remotePathId) {
        final RouteKey key = new RouteKey(routerId, remotePathId);
        return removeRoute(key, getOffsets().offsetOf(key));
    }

    @Override
    public MapEntryNode createValue(final NodeIdentifierWithPredicates routeId, final AddPathBestPath path) {
        return RouteEntryUtil.createSimpleRouteValue(routeId, path);
    }

    @Override
    public int addRoute(final UnsignedInteger routerId, final Long remotePathId, final NodeIdentifier attII,
            final NormalizedNode<?, ?> data) {
        return addRoute(new RouteKey(routerId, remotePathId), attII, data);
    }
}
