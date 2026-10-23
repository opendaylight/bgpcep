/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

@NonNullByDefault
final class AdjRibInTables {
    private final Map<TablesKey, AdjRibInTable> tables;

    private AdjRibInTables(final Map<TablesKey, AdjRibInTable> tables) {
        this.tables = Map.copyOf(tables);
    }

    static AdjRibInTables of(final RIBSupportContextRegistry registry, final Set<TablesKey> tables) {
        return null;
    }
}
