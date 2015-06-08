/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.primitives.UnsignedInteger;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple {@link AbstractRouteEntry} objects,
 * which share either contributors or consumers.
 *
 * We also provide utility reformat methods, which provide access to
 * array members and array management features.
 */
final class OffsetMap {
    static final OffsetMap EMPTY = new OffsetMap(Collections.<UnsignedInteger>emptySet());
    private static final LoadingCache<Set<UnsignedInteger>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Set<UnsignedInteger>, OffsetMap>() {
        @Override
        public OffsetMap load(final Set<UnsignedInteger> key) throws Exception {
            return new OffsetMap(key);
        }
    });
    private static final Comparator<UnsignedInteger> IPV4_COMPARATOR = new Comparator<UnsignedInteger>() {
        @Override
        public int compare(final UnsignedInteger o1, final UnsignedInteger o2) {
            return o1.compareTo(o2);
        }
    };
    private final UnsignedInteger[] routerIds;

    private OffsetMap(final Set<UnsignedInteger> routerIds) {
        final UnsignedInteger[] array = routerIds.toArray(new UnsignedInteger[0]);
        Arrays.sort(array, IPV4_COMPARATOR);
        this.routerIds = array;
    }

    UnsignedInteger getRouterId(final int offset) {
        Preconditions.checkArgument(offset >= 0);
        return this.routerIds[offset];
    }

    int offsetOf(final UnsignedInteger routerId) {
        return Arrays.binarySearch(this.routerIds, routerId, IPV4_COMPARATOR);
    }

    int size() {
        return this.routerIds.length;
    }

    OffsetMap with(final UnsignedInteger routerId) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<UnsignedInteger> b = ImmutableSet.builder();
        b.add(this.routerIds);
        b.add(routerId);

        return OFFSETMAPS.getUnchecked(b.build());
    }

    <T> T getValue(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routerIds.length, "Invalid offset %s for %s router IDs", offset, routerIds.length);
        return array[offset];
    }

    <T> void setValue(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routerIds.length, "Invalid offset %s for %s router IDs", offset, routerIds.length);
        array[offset] = value;
    }

    <T> T[] expand(final OffsetMap oldOffsets, final T[] oldArray, final int offset) {
        @SuppressWarnings("unchecked")
        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), this.routerIds.length);
        final int oldSize = oldOffsets.routerIds.length;

        System.arraycopy(oldArray, 0, ret, 0, offset);
        System.arraycopy(oldArray, offset, ret, offset + 1, oldSize - offset);

        return ret;
    }
}
