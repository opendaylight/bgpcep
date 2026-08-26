/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * Builds the AdjRibsOut entry a peer writes for one route.
 *
 * <p>What comes back always equals what
 * {@link RIBSupport#createRoute(MapEntryNode, NodeIdentifierWithPredicates, ContainerNode)} builds from the same
 * route, key and attributes, so a peer needs to know nothing about how it was obtained. It may be an entry other
 * peers hold too, so a peer must not modify it. Implementations accept calls from several threads at once.
 */
@NonNullByDefault
public sealed interface RibOutEntryFactory permits SharedRibOutEntries, UnsharedRibOutEntries {
    /**
     * Returns a new factory which reuses one entry for the peers whose entries come out equal. Take a new factory
     * for each update.
     *
     * @return a new factory which reuses entries
     */
    static RibOutEntryFactory newSharing() {
        return new SharedRibOutEntries();
    }

    /**
     * Returns a factory which builds every entry separately. Use it where a route reaches a single peer and there
     * is nothing to share with.
     *
     * @return a factory which reuses nothing
     */
    static RibOutEntryFactory unshared() {
        return UnsharedRibOutEntries.INSTANCE;
    }

    /**
     * Returns the entry to write for one route.
     *
     * @param ribSupport RIBSupport of the table being written
     * @param advRoute route being advertised
     * @param key key the entry is written under, which carries a path id for peers using add-path
     * @param effAttr attributes for this peer, as returned by {@code BGPRibRoutingPolicy.applyExportPolicies}
     * @return the entry to write, which other peers may hold as well
     */
    MapEntryNode entryFor(RIBSupport<?, ?> ribSupport, AbstractAdvertizedRoute<?, ?> advRoute,
        NodeIdentifierWithPredicates key, ContainerNode effAttr);
}
