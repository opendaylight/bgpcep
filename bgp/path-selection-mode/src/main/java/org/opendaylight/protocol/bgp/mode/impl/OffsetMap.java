/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
public final class OffsetMap<E extends Comparable<E>> {
    private static final Logger LOG = LoggerFactory.getLogger(OffsetMap.class);
    private static final String NEGATIVEOFFSET = "Invalid negative offset %s";
    private static final String INVALIDOFFSET = "Invalid offset %s for %s router IDs";
    private final Comparator<E> ipv4Comparator = E::compareTo;
    private final E[] routeKeys;
    private final Class<E> clazz;
    private final LoadingCache<Set<E>, OffsetMap<E>> keyCache = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Set<E>, OffsetMap<E>>() {
        @Override
        public OffsetMap<E> load(@Nonnull final Set<E> key) throws Exception {
            return new OffsetMap<>(key, clazz);
        }
    });

    public OffsetMap(final Class<E> clazz) {
        this.clazz = clazz;
        this.routeKeys = (E[]) Array.newInstance(clazz, 0);
    }

    private OffsetMap(final Set<E> keysSet, final Class<E> clazz) {
        this.clazz = clazz;
        final E[] array = keysSet.toArray((E[]) Array.newInstance(this.clazz, keysSet.size()));
        Arrays.sort(array, ipv4Comparator);
        this.routeKeys = array;
    }

    public List<E> getRouteKeysList() {
        return Arrays.stream(this.routeKeys).collect(Collectors.toList());
    }

    public E getRouterKey(final int offset) {
        Preconditions.checkArgument(offset >= 0);
        return this.routeKeys[offset];
    }

    public int offsetOf(final E key) {
        return Arrays.binarySearch(this.routeKeys, key, ipv4Comparator);
    }

    public int size() {
        return this.routeKeys.length;
    }

    public OffsetMap<E> with(final E key) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<E> b = ImmutableSet.builder();
        b.add(this.routeKeys);
        b.add(key);

        return keyCache.getUnchecked(b.build());
    }

    public OffsetMap<E> without(final E key) {
        final Builder<E> b = ImmutableSet.builder();
        int index = indexOfRouterId(key);
        if (index < 0) {
            LOG.trace("Router key not found", key);
        } else {
            b.add(removeValue(this.routeKeys, index));
        }
        return keyCache.getUnchecked(b.build());
    }

    private int indexOfRouterId(final E key) {
        for (int i = 0; i < this.routeKeys.length; i++) {
            if (key.equals(this.routeKeys[i])) {
                return i;
            }
        }
        return -1;
    }

    public <T> T getValue(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < routeKeys.length, INVALIDOFFSET, offset, routeKeys.length);
        return array[offset];
    }

    public <T> void setValue(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < routeKeys.length, INVALIDOFFSET, offset, routeKeys.length);
        array[offset] = value;
    }

    public <T> T[] expand(final OffsetMap oldOffsets, final T[] oldArray, final int offset) {
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
        Preconditions.checkArgument(offset < routeKeys.length, INVALIDOFFSET, offset, length);

        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), length - 1);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < length - 1) {
            System.arraycopy(oldArray, offset + 1, ret, offset, length - offset - 1);
        }

        return ret;
    }

    public boolean isEmty() {
        return this.size() == 0;
    }
}
