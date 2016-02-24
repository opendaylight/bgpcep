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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple {@link AbstractRouteEntry} objects,
 * which share either contributors or consumers.
 * <p/>
 * We also provide utility reformat methods, which provide access to
 * array members and array management features.
 */
final class OffsetMap {
    private static final Logger LOG = LoggerFactory.getLogger(OffsetMap.class);
    static final OffsetMap EMPTY = new OffsetMap(Collections.<String>emptySet());
    private static final LoadingCache<Set<String>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Set<String>, OffsetMap>() {
        @Override
        public OffsetMap load(final Set<String> key) throws Exception {
            return new OffsetMap(key);
        }
    });
    private static final Comparator<String> IPV4_COMPARATOR = (o1, o2) -> o1.compareTo(o2);
    private final String[] routeKeys;

    private OffsetMap(final Set<String> routerIds) {
        this.routeKeys = routerIds.size() < 1 ? new String[0] : routerIds.stream().sorted(IPV4_COMPARATOR).toArray(String[]::new);
    }

    public List<String> getRouteKeysList(){
        return Arrays.stream(this.routeKeys).collect(Collectors.toList());
    }

    String getRouterKey(final int offset) {
        Preconditions.checkArgument(offset >= 0);
        return this.routeKeys[offset];
    }

    int offsetOf(final String key)
    {
        return Arrays.binarySearch(this.routeKeys, key, IPV4_COMPARATOR);
    }

    int size() {
        return this.routeKeys.length;
    }

    OffsetMap with(final String key) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<String> b = ImmutableSet.builder();
        b.add(this.routeKeys);
        b.add(key);

        return OFFSETMAPS.getUnchecked(b.build());
    }

    OffsetMap without(final String key) {
        final Builder<String> b = ImmutableSet.builder();
        int index = indexOfRouterId(key);
        if (index < 0) {
            LOG.trace("Router key not found", key);
        }
        b.add(removeValue(this.routeKeys, index));
        return OFFSETMAPS.getUnchecked(b.build());
    }

    private int indexOfRouterId(final String key) {
        for (int i = 0; i < this.routeKeys.length; i++) {
            if (key.equals(this.routeKeys[i])) {
                return i;
            }
        }
        return -1;
    }

    <T> T getValue(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routeKeys.length, "Invalid offset %s for %s router IDs", offset, routeKeys.length);
        return array[offset];
    }

    <T> void setValue(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routeKeys.length, "Invalid offset %s for %s router IDs", offset, routeKeys.length);
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

    <T> T[] removeValue(final T[] oldArray, final int offset) {
        final int length = oldArray.length;
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset %s", offset);
        Preconditions.checkArgument(offset < routeKeys.length, "Invalid offset %s for %s router IDs", offset, length);

        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), length - 1);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < length - 1) {
            System.arraycopy(oldArray, offset + 1, ret, offset, length - offset - 1);
        }

        return ret;
    }
}
