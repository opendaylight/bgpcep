/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AddAllRouteEntry extends AddPathRouteEntry {
    private static final Logger LOG = LoggerFactory.getLogger(AddAllRouteEntry.class);

    @Override
    List<AddPathBestPath> selectBest(final RIBSupport ribSupport, final long localAs, final int size) {
        // Select the best path for the case when AddPath is not supported
        final var selector = new AddPathSelector(localAs);
        for (int offset = 0; offset < size; ++offset) {
            processOffset(ribSupport, selector, offset);
        }

        final var newBest = selector.result();
        LOG.trace("Best path selected {}", newBest);

        final var paths = new ArrayList<AddPathBestPath>(size);
        paths.add(newBest);

        // Since we are selecting all paths, add all the other paths, do that in two steps, skipping the selected
        // route.
        for (int offset = 0; offset < newBest.getOffset(); ++offset) {
            paths.add(bestPathAt(ribSupport, offset));
        }
        for (int offset = newBest.getOffset() + 1; offset < size; ++offset) {
            paths.add(bestPathAt(ribSupport, offset));
        }
        return paths;
    }
}
