/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.mode.impl.BestPathStateImpl;
import org.opendaylight.protocol.bgp.mode.spi.AbstractBestPathSelector;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AddPathSelector extends AbstractBestPathSelector {
    private static final Logger LOG = LoggerFactory.getLogger(AddPathSelector.class);

    private RouteKey bestRouteKey;
    private Uint32 bestPathId;
    private int bestOffset;

    public AddPathSelector(final long ourAs) {
        super(ourAs);
    }

    void processPath(final Attributes attrs, final RouteKey key, final int offsetPosition, final Uint32 pathId) {
        // Consider only non-null attributes
        if (attrs != null) {
            final RouterId routerId = key.getRouterId();
            final RouterId originatorId = replaceOriginator(routerId, attrs.getOriginatorId());

            /*
             * Store the new details if we have nothing stored or when the selection algorithm indicates new details
             * are better.
             */
            final BestPathState state = new BestPathStateImpl(attrs);
            if (this.bestOriginatorId == null || !isExistingPathBetter(state)) {
                LOG.trace("Selecting path from router {}", routerId);
                this.bestOriginatorId = originatorId;
                this.bestState = state;
                this.bestRouteKey = key;
                this.bestOffset = offsetPosition;
                this.bestPathId = pathId;
            }
        }
    }

    public AddPathBestPath result() {
        return this.bestRouteKey == null ? null : new AddPathBestPath(this.bestState, this.bestRouteKey,
            this.bestPathId, this.bestOffset);
    }
}
