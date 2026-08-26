/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

/**
 * {@link RibOutEntryFactory#newSharing()} implementation. Keeps the entry built for each route key together with
 * the route it came from, and hands it to the next peer asking for that same key and route.
 *
 * <p>The key carries a path id for peers using add-path, so those share among themselves and the rest share among
 * themselves.
 *
 * <p>A route key stays the same across updates while the route it identifies changes, so a stored entry is only
 * valid for the route it was built from. That is why the route is compared before a stored entry is used, and why
 * a factory kept beyond one update rebuilds instead of returning an entry for a route that has changed.
 */
@NonNullByDefault
final class SharedRibOutEntries implements RibOutEntryFactory {
    private record CachedEntry(AbstractAdvertizedRoute<?, ?> advRoute, MapEntryNode entry) {
        CachedEntry {
            requireNonNull(advRoute);
            requireNonNull(entry);
        }
    }

    private final ConcurrentMap<NodeIdentifierWithPredicates, CachedEntry> shared = new ConcurrentHashMap<>();

    SharedRibOutEntries() {
        // Use RibOutEntryFactory.newSharing()
    }

    @Override
    public MapEntryNode entryFor(final RIBSupport<?, ?> ribSupport, final AbstractAdvertizedRoute<?, ?> advRoute,
            final NodeIdentifierWithPredicates key, final ContainerNode effAttr) {
        final var route = advRoute.getRoute();
        if (effAttr != advRoute.getAttributes()) {
            // Reference comparison. A caller passes advRoute.getAttributes() straight through when the export
            // policy left the route alone, so a different object means the policy produced these attributes for
            // this peer. Storing it would hand it to every other peer writing this route, whatever their own
            // policy produced.
            return ribSupport.createRoute(route, key, effAttr);
        }

        // At most one build per key even when peers race here, which is the duplication this class avoids.
        final var cached = shared.computeIfAbsent(key, sharedKey -> new CachedEntry(advRoute,
            ribSupport.createRoute(route, sharedKey, effAttr)));
        // Only possible when this instance is reused for a second update, so the map still holds an entry
        // from the first. Overwriting it means the remaining peers of this update find the new entry.
        if (cached.advRoute() != advRoute) {
            final var fresh = new CachedEntry(advRoute, ribSupport.createRoute(route, key, effAttr));
            shared.put(key, fresh);
            return fresh.entry();
        }
        return cached.entry();
    }
}
