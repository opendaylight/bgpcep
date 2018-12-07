/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.add;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import org.opendaylight.protocol.bgp.mode.impl.AbstractOffsetMap;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple RouteEntry objects,
 * which share either contributors or consumers.
 * We also provide utility reformat methods, which provide access to
 * array members and array management features.
 */
final class RouteKeyOffsets extends AbstractOffsetMap<RouteKey, RouteKeyOffsets> {
    private static final LoadingCache<ImmutableSet<RouteKey>, RouteKeyOffsets> OFFSETMAPS = CacheBuilder.newBuilder()
            .weakValues().build(new CacheLoader<ImmutableSet<RouteKey>, RouteKeyOffsets>() {
                @Override
                public RouteKeyOffsets load(final ImmutableSet<RouteKey> key) {
                    return new RouteKeyOffsets(key);
                }
            });
    private static final Comparator<RouteKey> COMPARATOR = RouteKey::compareTo;
    private static final RouteKey[] EMPTY_KEYS = new RouteKey[0];

    static final RouteKeyOffsets EMPTY = new RouteKeyOffsets(ImmutableSet.of());

    private RouteKeyOffsets(final ImmutableSet<RouteKey> routeKeys) {
        super(EMPTY_KEYS, COMPARATOR, routeKeys);
    }

    @Override
    protected Comparator<RouteKey> comparator() {
        return COMPARATOR;
    }

    @Override
    protected RouteKey[] emptyKeys() {
        return EMPTY_KEYS;
    }

    @Override
    protected RouteKeyOffsets instanceForKeys(final ImmutableSet<RouteKey> newKeys) {
        return OFFSETMAPS.getUnchecked(newKeys);
    }
}