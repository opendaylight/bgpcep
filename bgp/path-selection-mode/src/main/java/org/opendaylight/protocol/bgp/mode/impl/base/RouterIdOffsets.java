/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import org.opendaylight.protocol.bgp.mode.impl.AbstractOffsetMap;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;

/**
 * A map of {@link RouterId} to an offset.
 *
 * @see AbstractOffsetMap
 */
final class RouterIdOffsets extends AbstractOffsetMap<RouterId, RouterIdOffsets> {
    private static final LoadingCache<ImmutableSet<RouterId>, RouterIdOffsets> OFFSETMAPS = CacheBuilder.newBuilder()
            .weakValues().build(new CacheLoader<ImmutableSet<RouterId>, RouterIdOffsets>() {
                @Override
                public RouterIdOffsets load(final ImmutableSet<RouterId> key) {
                    return new RouterIdOffsets(key);
                }
            });
    private static final Comparator<RouterId> COMPARATOR = RouterId::compareTo;
    private static final RouterId[] EMPTY_KEYS = new RouterId[0];

    static final RouterIdOffsets EMPTY = new RouterIdOffsets(ImmutableSet.of());

    RouterIdOffsets(final ImmutableSet<RouterId> routerIds) {
        super(EMPTY_KEYS, COMPARATOR, routerIds);
    }

    @Override
    protected Comparator<RouterId> comparator() {
        return COMPARATOR;
    }

    @Override
    protected RouterId[] emptyKeys() {
        return EMPTY_KEYS;
    }

    @Override
    protected RouterIdOffsets instanceForKeys(final ImmutableSet<RouterId> newKeys) {
        return OFFSETMAPS.getUnchecked(newKeys);
    }
}
