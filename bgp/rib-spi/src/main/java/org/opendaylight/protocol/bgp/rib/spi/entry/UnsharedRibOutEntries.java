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
 * {@link RibOutEntryFactory#unshared()} implementation. Calls
 * {@link RIBSupport#createRoute(MapEntryNode, NodeIdentifierWithPredicates, ContainerNode)} for every entry, which
 * allocates one each time.
 *
 * <p>It keeps nothing between calls, which is why {@link RibOutEntryFactory#unshared()} can hand out one instance
 * to every caller instead of building a factory each time.
 */
@NonNullByDefault
final class UnsharedRibOutEntries implements RibOutEntryFactory {
    static final UnsharedRibOutEntries INSTANCE = new UnsharedRibOutEntries();

    private UnsharedRibOutEntries() {
        // Use RibOutEntryFactory.unshared()
    }

    @Override
    public MapEntryNode entryFor(final RIBSupport<?, ?> ribSupport, final AbstractAdvertizedRoute<?, ?> advRoute,
            final NodeIdentifierWithPredicates key, final ContainerNode effAttr) {
        return ribSupport.createRoute(advRoute.getRoute(), key, effAttr);
    }
}
