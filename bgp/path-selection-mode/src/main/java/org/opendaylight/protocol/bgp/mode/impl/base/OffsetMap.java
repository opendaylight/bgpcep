/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple {@link BaseAbstractRouteEntry} objects,
 * which share either contributors or consumers.
 * We also provide utility reformat methods, which provide access to
 * array members and array management features.
 */
final class OffsetMap {
    static final OffsetMap EMPTY = new OffsetMap(Collections.<UnsignedInteger>emptySet());
    private static final Logger LOG = LoggerFactory.getLogger(OffsetMap.class);
    private static final LoadingCache<Set<UnsignedInteger>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Set<UnsignedInteger>, OffsetMap>() {
        @Override
        public OffsetMap load(final Set<UnsignedInteger> key) throws Exception {
            return new OffsetMap(key);
        }
    });
    private static final Comparator<UnsignedInteger> IPV4_COMPARATOR = UnsignedInteger::compareTo;
    private final UnsignedInteger[] routerIds;

    private OffsetMap(final Set<UnsignedInteger> routerIds) {
        this.routerIds = routerIds.isEmpty() ? new UnsignedInteger[0] : routerIds.stream().sorted(IPV4_COMPARATOR).toArray(UnsignedInteger[]::new);
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

    boolean isEmty() {
        return this.routerIds.length == 0;
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

    OffsetMap without(final UnsignedInteger routerId) {
        final Builder<UnsignedInteger> b = ImmutableSet.builder();
        int index = indexOfRouterId(routerId);
        if (index < 0) {
            LOG.trace("RouterId not found", routerId);
        }
        b.add(removeValue(this.routerIds, index));
        return OFFSETMAPS.getUnchecked(b.build());
    }

    private int indexOfRouterId(final UnsignedInteger routerId) {
        final int startIndex = 0;
        for (int i = startIndex; i < this.routerIds.length; i++) {
            if (routerId.equals(this.routerIds[i])) {
                return i;
            }
        }
        return -1;
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

    <T> T[] removeValue(final T[] oldArray, final int offset) {
        final int length = oldArray.length;
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routerIds.length, "Invalid offset %s for %s router IDs", offset, length);

        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), length - 1);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < length - 1) {
            System.arraycopy(oldArray, offset + 1, ret, offset, length - offset - 1);
        }

        return ret;
    }
}
