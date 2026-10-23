/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
final class AdjRibInTables {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInTables.class);

    static final AdjRibInTables EMPTY = new AdjRibInTables(Map.of());

    private final Map<TablesKey, AdjRibInTable> tables;

    private AdjRibInTables(final Map<TablesKey, AdjRibInTable> tables) {
        this.tables = Map.copyOf(tables);
    }

    static AdjRibInTables of(final YangInstanceIdentifier peerPath, final Set<TablesKey> tableTypes,
            final Function<TablesKey, @Nullable RIBSupportContext> supportProvider) {
        final var size = tableTypes.size();
        if (size == 0) {
            return EMPTY;
        }

        final var tmp = HashMap.<TablesKey, AdjRibInTable>newHashMap(size);
        for (var tableType : tableTypes) {
            final var tableSupport = supportProvider.apply(tableType);
            if (tableSupport != null) {
                tmp.put(tableType, AdjRibInTable.ofPeer(tableSupport, peerPath));
            }
        }
        return new AdjRibInTables(tmp);
    }

    void forEach(final BiConsumer<TablesKey, AdjRibInTable> action) {
        tables.forEach(action);
    }

    @Nullable AdjRibInTable tableOf(final TablesKey tableType) {
        return tables.get(tableType);
    }

    AdjRibInTable getTableOf(final TablesKey tableType) {
        final var table = tableOf(tableType);
        if (table == null) {
            throw new NoSuchElementException("No table for " + tableType);
        }
        return table;
    }
}
