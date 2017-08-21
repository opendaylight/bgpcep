/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer.route;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerTableTypeRouteCounter {
    private static final Logger LOG = LoggerFactory.getLogger(PerTableTypeRouteCounter.class);

    private final Map<TablesKey, LongAdder> counters = new ConcurrentHashMap<>();

    public PerTableTypeRouteCounter(@Nonnull final Set<TablesKey> tablesKeySet) {
        init(tablesKeySet);
    }

    public PerTableTypeRouteCounter() {
    }

    private synchronized void init(@Nonnull final Set<TablesKey> tablesKeySet) {
        tablesKeySet.forEach(this::init);
    }

    public final synchronized LongAdder init(@Nonnull final TablesKey tablesKey) {
        LongAdder counter = this.counters.get(requireNonNull(tablesKey));
        if (counter == null) {
            counter = new LongAdder();
            this.counters.put(tablesKey, counter);
        }
        LOG.debug("Initializing route counter for tablesKey {}", tablesKey);
        counter.reset();
        return counter;
    }

    /**
     * Get the counter for given tablesKey. Return an empty counter if it doesn't exist
     * NOTE: the created empty counter won't be put into the original map
     *
     * @param tablesKey
     */
    @Nonnull public final LongAdder getCounterOrDefault(@Nonnull final TablesKey tablesKey) {
        return this.counters.getOrDefault(requireNonNull(tablesKey), new LongAdder());
    }

    /**
     * Get the counter with given tablesKey.  Create an empty counter if it doesn't exist
     * This method will put the created empty counter back to map
     *
     * @param tablesKey
     */
    public final LongAdder getCounterOrSetDefault(@Nonnull final TablesKey tablesKey) {
        if (!this.counters.containsKey(tablesKey)) {
            return init(tablesKey);
        }

        return this.counters.get(requireNonNull(tablesKey));
    }

    public final Map<TablesKey, LongAdder> getCounters() {
        return this.counters;
    }

    public final void resetAll() {
        LOG.debug("Resetting all route counters..");
        this.counters.values().forEach(LongAdder::reset);
    }

    public void setValueToCounterOrSetDefault(final TablesKey tablesKey, final int size) {
        final LongAdder counter = getCounterOrSetDefault(tablesKey);
        counter.reset();
        counter.add(size);
    }
}
