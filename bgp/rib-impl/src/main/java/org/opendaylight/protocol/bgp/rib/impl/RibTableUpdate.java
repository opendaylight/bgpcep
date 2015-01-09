/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single batch of {@link RibTable} updates.
 *
 * @param <T> NLRI type
 */
final class RibTableUpdate<T> {
    private static final class Entry<T> implements Map.Entry<T, RibTableEntry<T>> {
        private final T key;
        private final RibTableEntry<T> value;

        Entry(final T key, final RibTableEntry<T> value) {
            this.key = Preconditions.checkNotNull(key);
            this.value = Preconditions.checkNotNull(value);
        }

        @Override
        public T getKey() {
            return key;
        }

        @Override
        public RibTableEntry<T> getValue() {
            return value;
        }

        @Override
        public RibTableEntry<T> setValue(final RibTableEntry<T> value) {
            throw new UnsupportedOperationException();
        }

    }
    private static final Logger LOG = LoggerFactory.getLogger(RibTableUpdate.class);
    private final Collection<Entry<T>> entries;
    private final RibTable<T> table;

    RibTableUpdate(final RibTable<T> table, final int size) {
        this.table = Preconditions.checkNotNull(table);
        entries = new ArrayList<>();
    }

    void selectBestPaths(@Nonnull final AsNumber localAs) {
        LOG.debug("Processing {} table entries", entries.size());
        for (Entry<T> e : entries) {
            table.selectBestPath(localAs, e.getKey(), e.getValue());
        }
    }

    void add(@Nonnull final T route, @Nonnull final RibTableEntry<T> entry) {
        entries.add(new Entry<>(route, entry));
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }
}
