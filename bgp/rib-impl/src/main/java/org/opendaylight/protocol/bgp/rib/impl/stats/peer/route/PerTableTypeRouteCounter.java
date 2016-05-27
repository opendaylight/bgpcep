/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer.route;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public final class PerTableTypeRouteCounter {
    private static final Logger LOG = LoggerFactory.getLogger(PerTableTypeRouteCounter.class);

    private final Map<TablesKey, RouteCounter> counters = new HashMap<>();

    public final synchronized RouteCounter init(final TablesKey tablesKey) {
        LOG.debug("Route counter is created for tablesKey {}", tablesKey);
        final RouteCounter counter = new RouteCounter();
        this.counters.put(tablesKey, counter);
        return counter;
    }

    public final RouteCounter getCounter(final TablesKey tablesKey) {
        final RouteCounter counter = this.counters.get(tablesKey);
        Preconditions.checkNotNull(counter);
        return counter;
    }

    public final Map<TablesKey, RouteCounter> getCounters() {
        return this.counters;
    }

    public final void resetAll() {
        LOG.debug("Route counter is reset");
        this.counters.clear();
    }
}
