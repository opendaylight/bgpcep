/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPathSelector;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AddPathSelector extends AbstractBestPathSelector {
    private static final Logger LOG = LoggerFactory.getLogger(AddPathSelector.class);

    private RouteKey bestRouteKey;
    private Uint32 bestPathId;
    private int bestOffset;

    AddPathSelector(final long ourAs) {
        super(ourAs);
    }

    void processPath(final ContainerNode attrs, final RouteKey key, final int offsetPosition, final Uint32 pathId) {
        // Consider only non-null attributes
        if (attrs == null) {
            return;
        }

        final var routerId = key.routerId();
        final var originatorId = replaceOriginator(routerId, attrs);

        // store the new details if we have nothing stored or when the selection algorithm indicates new details are
        // better
        final var state = new BestPathStateImpl(attrs);
        if (bestOriginatorId == null || !isExistingPathBetter(state)) {
            LOG.trace("Selecting path from router {}", routerId);
            bestOriginatorId = originatorId;
            bestState = state;
            bestOffset = offsetPosition;
            bestPathId = pathId;
            bestRouteKey = key;
        }
    }

    AddPathBestPath result() {
        return bestRouteKey == null ? null : new AddPathBestPath(bestState, bestRouteKey, bestPathId, bestOffset);
    }
}
