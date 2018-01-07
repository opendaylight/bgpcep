/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.protocol.bgp.mode.spi.RouteEntryUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

final class BaseSimpleRouteEntry extends BaseAbstractRouteEntry {
    @Override
    public boolean removeRoute(UnsignedInteger routerId, final Long remotePathId) {
        return removeRoute(routerId, getOffsets().offsetOf(routerId));
    }

    @Override
    public MapEntryNode createValue(final NodeIdentifierWithPredicates routeId, final BaseBestPath path) {
        return RouteEntryUtil.createSimpleRouteValue(routeId, path);
    }
}
