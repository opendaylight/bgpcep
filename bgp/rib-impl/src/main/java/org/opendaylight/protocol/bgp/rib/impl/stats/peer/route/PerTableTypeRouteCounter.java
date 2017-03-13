/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer.route;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerTableTypeRouteCounter {
    private static final Logger LOG = LoggerFactory.getLogger(PerTableTypeRouteCounter.class);

    private final Map<TablesKey, UnsignedInt32Counter> counters = new ConcurrentHashMap<>();
    private final String counterName;

    private final UnsignedInt32Counter createCounter(@Nonnull final TablesKey tablesKey) {
        return new UnsignedInt32Counter(this.counterName + tablesKey.toString());
    }

    public PerTableTypeRouteCounter(@Nonnull final String counterName) {
        this.counterName = Preconditions.checkNotNull(counterName);
    }

    public PerTableTypeRouteCounter(@Nonnull final String counterName, @Nonnull final Set<TablesKey> tablesKeySet) {
        this(counterName);
        init(tablesKeySet);
    }

    public final synchronized void init(@Nonnull Set<TablesKey> tablesKeySet) {
        tablesKeySet.stream().forEach(this::init);
    }

    public final synchronized UnsignedInt32Counter init(@Nonnull final TablesKey tablesKey) {
        UnsignedInt32Counter counter = this.counters.get(Preconditions.checkNotNull(tablesKey));
        if (counter == null) {
            this.counters.put(tablesKey, counter = createCounter(tablesKey));
        }
        LOG.debug("Initializing route counter for tablesKey {}", tablesKey);
        counter.resetCount();
        return counter;
    }

    /**
     * Get the counter for given tablesKey. Return an empty counter if it doesn't exist
     * NOTE: the created empty counter won't be put into the original map
     * @param tablesKey
     * @return
     */
    @Nonnull public final UnsignedInt32Counter getCounterOrDefault(@Nonnull final TablesKey tablesKey) {
        return this.counters.getOrDefault(Preconditions.checkNotNull(tablesKey), createCounter(tablesKey));
    }

    /**
     * Get the counter with given tablesKey.  Create an empty counter if it doesn't exist
     * This method will put the created empty counter back to map
     * @param tablesKey
     * @return
     */
    public final UnsignedInt32Counter getCounterOrSetDefault(@Nonnull final TablesKey tablesKey) {
        if (!this.counters.containsKey(tablesKey)) {
            return init(tablesKey);
        } else {
            return this.counters.get(Preconditions.checkNotNull(tablesKey));
        }
    }

    public final Map<TablesKey, UnsignedInt32Counter> getCounters() {
        return this.counters;
    }

    public final void resetAll() {
        LOG.debug("Resetting all route counters..");
        this.counters.values().stream().forEach(UnsignedInt32Counter::resetCount);
    }
}
