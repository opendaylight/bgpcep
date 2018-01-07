/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple RouteEntry objects,
 * which share either contributors or consumers.
 * We also provide utility reformat methods, which provide access to
 * array members and array management features.
 */
final class OffsetMap {
    private static final Logger LOG = LoggerFactory.getLogger(OffsetMap.class);
    private static final String NEGATIVEOFFSET = "Invalid negative offset %s";
    private static final String INVALIDOFFSET = "Invalid offset %s for %s router IDs";
    private static final LoadingCache<Set<UnsignedInteger>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder().weakValues().build(
        new CacheLoader<Set<UnsignedInteger>, OffsetMap>() {
            @Override
            public OffsetMap load(@Nonnull final Set<UnsignedInteger> key) throws Exception {
                return new OffsetMap(key);
            }
        });
    private static final Comparator<UnsignedInteger> COMPARATOR = UnsignedInteger::compareTo;
    static final OffsetMap EMPTY = new OffsetMap(Collections.emptySet());
    private final UnsignedInteger[] routeKeys;

    private OffsetMap(final Set<UnsignedInteger> routerIds) {
        final UnsignedInteger[] array = routerIds.toArray(new UnsignedInteger[0]);
        Arrays.sort(array, COMPARATOR);
        this.routeKeys = array;
    }

    UnsignedInteger getRouterKey(final int offset) {
        Preconditions.checkArgument(offset >= 0);
        return this.routeKeys[offset];
    }

    public int offsetOf(final UnsignedInteger key) {
        return Arrays.binarySearch(this.routeKeys, key, COMPARATOR);
    }

    public int size() {
        return this.routeKeys.length;
    }

    public OffsetMap with(final UnsignedInteger key) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<UnsignedInteger> builder = ImmutableSet.builder();
        builder.add(this.routeKeys);
        builder.add(key);

        return OFFSETMAPS.getUnchecked(builder.build());
    }

    public OffsetMap without(final UnsignedInteger key) {
        final Builder<UnsignedInteger> builder = ImmutableSet.builder();
        final int index = indexOfRouterId(key);
        if (index < 0) {
            LOG.trace("Router key not found", key);
        } else {
            builder.add(removeValue(this.routeKeys, index));
        }
        return OFFSETMAPS.getUnchecked(builder.build());
    }

    private int indexOfRouterId(final UnsignedInteger key) {
        for (int i = 0; i < this.routeKeys.length; i++) {
            if (key.equals(this.routeKeys[i])) {
                return i;
            }
        }
        return -1;
    }

    public <T> T getValue(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routeKeys.length, INVALIDOFFSET, offset, this.routeKeys.length);
        return array[offset];
    }

    public <T> void setValue(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routeKeys.length, INVALIDOFFSET, offset, this.routeKeys.length);
        array[offset] = value;
    }

    <T> T[] expand(final OffsetMap oldOffsets, final T[] oldArray, final int offset) {
        @SuppressWarnings("unchecked")
        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), this.routeKeys.length);
        final int oldSize = oldOffsets.routeKeys.length;

        System.arraycopy(oldArray, 0, ret, 0, offset);
        System.arraycopy(oldArray, offset, ret, offset + 1, oldSize - offset);

        return ret;
    }

    public <T> T[] removeValue(final T[] oldArray, final int offset) {
        final int length = oldArray.length;
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routeKeys.length, INVALIDOFFSET, offset, length);

        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), length - 1);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < length - 1) {
            System.arraycopy(oldArray, offset + 1, ret, offset, length - offset - 1);
        }

        return ret;
    }

    boolean isEmpty() {
        return this.size() == 0;
    }
}