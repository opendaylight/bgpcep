/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract sealed class PrefixCounters {
    /**
     * {@link PrefixesInstalledCounters} based on a map of {@link LongAdder}s.
     */
    static final class Installed extends PrefixCounters implements PrefixesInstalledCounters {
        Installed(final Set<TablesKey> tables) {
            super(tables);
        }

        @Override
        public long getPrefixedInstalledCount(final TablesKey tablesKey) {
            return read(tablesKey);
        }

        @Override
        public long getTotalPrefixesInstalled() {
            return sum();
        }

        @Override
        public String toString() {
            return toString(PrefixesInstalledCounters.class);
        }
    }

    /**
     * {@link PrefixesReceivedCounters} based on a map of {@link LongAdder}s.
     */
    static final class Received extends PrefixCounters implements PrefixesReceivedCounters {
        Received(final Set<TablesKey> tables) {
            super(tables);
        }

        @Override
        public long getPrefixedReceivedCount(final TablesKey tablesKey) {
            return read(tablesKey);
        }

        @Override
        public Set<TablesKey> getTableKeys() {
            return null;
        }

        @Override
        public boolean isSupported(final TablesKey tablesKey) {
            return false;
        }

        @Override
        public String toString() {
            return toString(PrefixesReceivedCounters.class);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PrefixCounters.class);

    private final ImmutableMap<TablesKey, LongAdder> tableToAdder;

    private PrefixCounters(final Set<TablesKey> tables) {
        tableToAdder = tables.stream()
            // preserve ordering of TablesKey as keySet()
            .collect(ImmutableMap.toImmutableMap(Function.identity(), unused -> new LongAdder()));
    }

    final void decrement(final TablesKey table) {
        add(table, -1);
    }

    final void decrement(final TablesKey table, final int delta) {
        add(table, -delta);
    }

    final void increment(final TablesKey table) {
        add(table, 1);
    }

    private void add(final TablesKey table, final int delta) {
        final var adder = tableToAdder.get(table);
        if (adder != null) {
            adder.add(delta);
        } else {
            LOG.warn("Family {} not supported", table);
        }
    }

    final long read(final TablesKey table) {
        final var counter = tableToAdder.get(table);
        return counter == null ? 0 : counter.longValue();
    }

    final long sum() {
        return tableToAdder.values().stream().mapToLong(LongAdder::longValue).sum();
    }

    final void clear() {
        tableToAdder.values().forEach(LongAdder::reset);
    }

    @Override
    public abstract String toString();

    final String toString(final Class<?> contract) {
        return MoreObjects.toStringHelper(contract).add("size", tableToAdder.size()).add("sum", sum()).toString();
    }
}