/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * new Routes to be advertized.
 *
 * @see BestPath#toAdvertized(RIBSupport, MapEntryNode, boolean)
 * @author Claudio D. Gasparini
 */
@NonNullByDefault
public final class AdvertizedRoute extends AbstractAdvertizedRoute {
    private final boolean isFirstBestPath;

    AdvertizedRoute(final RIBSupport ribSupport, final MapEntryNode route, final BestPath path,
            final boolean isFirstBestPath) {
        super(ribSupport, route, path);
        this.isFirstBestPath = isFirstBestPath;
    }

    public boolean isFirstBestPath() {
        return isFirstBestPath;
    }
}
