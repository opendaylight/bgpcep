/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPathSelector;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BasePathSelector extends AbstractBestPathSelector {
    private static final Logger LOG = LoggerFactory.getLogger(BasePathSelector.class);

    private RouterId bestRouterId = null;

    BasePathSelector(final long ourAs) {
        super(ourAs);
    }

    void processPath(final RouterId routerId, final ContainerNode attrs) {
        requireNonNull(routerId, "Router ID may not be null");

        // Consider only non-null attributes
        if (attrs != null) {
            final RouterId originatorId = replaceOriginator(routerId, attrs);
            /*
             * Store the new details if we have nothing stored or when the selection algorithm indicates new details
             * are better.
             */
            final BestPathState state = new BestPathStateImpl(attrs);
            if (bestOriginatorId == null || !isExistingPathBetter(state)) {
                LOG.trace("Selecting path from router {}", routerId);
                bestOriginatorId = originatorId;
                bestRouterId = routerId;
                bestState = state;
            }
        }
    }

    BaseBestPath result() {
        return bestRouterId == null ? null : new BaseBestPath(bestRouterId, bestState);
    }
}