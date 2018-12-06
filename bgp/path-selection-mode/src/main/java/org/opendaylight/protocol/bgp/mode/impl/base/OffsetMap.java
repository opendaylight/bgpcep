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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
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
    private static final LoadingCache<Set<RouterId>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder()
            .weakValues().build(new CacheLoader<Set<RouterId>, OffsetMap>() {
                @Override
                public OffsetMap load(@Nonnull final Set<RouterId> key) {
                    return new OffsetMap(key);
                }
            });
    private static final Comparator<RouterId> COMPARATOR = RouterId::compareTo;
    private static final RouterId[] EMPTY_KEYS = new RouterId[0];
    static final OffsetMap EMPTY = new OffsetMap(Collections.emptySet());

    private final RouterId[] routerKeys;

    private OffsetMap(final Set<RouterId> routerIds) {
        final RouterId[] array = routerIds.toArray(EMPTY_KEYS);
        Arrays.sort(array, COMPARATOR);
        this.routerKeys = array;
    }

    RouterId getRouterKey(final int offset) {
        Preconditions.checkArgument(offset >= 0);
        return this.routerKeys[offset];
    }

    public int offsetOf(final RouterId key) {
        return Arrays.binarySearch(this.routerKeys, key, COMPARATOR);
    }

    public int size() {
        return this.routerKeys.length;
    }

    public OffsetMap with(final RouterId key) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<RouterId> builder = ImmutableSet.builder();
        builder.add(this.routerKeys);
        builder.add(key);

        return OFFSETMAPS.getUnchecked(builder.build());
    }

    public OffsetMap without(final RouterId key) {
        final Builder<RouterId> builder = ImmutableSet.builder();
        final int index = indexOfRouterId(key);
        if (index < 0) {
            LOG.trace("Router key {} not found", key);
        } else {
            builder.add(removeValue(this.routerKeys, index, EMPTY_KEYS));
        }
        return OFFSETMAPS.getUnchecked(builder.build());
    }

    private int indexOfRouterId(final RouterId key) {
        for (int i = 0; i < this.routerKeys.length; i++) {
            if (key.equals(this.routerKeys[i])) {
                return i;
            }
        }
        return -1;
    }

    public <T> T getValue(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routerKeys.length, INVALIDOFFSET, offset, this.routerKeys.length);
        return array[offset];
    }

    public <T> void setValue(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routerKeys.length, INVALIDOFFSET, offset, this.routerKeys.length);
        array[offset] = value;
    }

    <T> T[] expand(final OffsetMap oldOffsets, final T[] oldArray, final int offset) {
        @SuppressWarnings("unchecked")
        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), this.routerKeys.length);
        final int oldSize = oldOffsets.routerKeys.length;

        System.arraycopy(oldArray, 0, ret, 0, offset);
        System.arraycopy(oldArray, offset, ret, offset + 1, oldSize - offset);

        return ret;
    }

    public <T> T[] removeValue(final T[] oldArray, final int offset, final T[] emptyArray) {
        final int length = oldArray.length;
        Preconditions.checkArgument(offset >= 0, NEGATIVEOFFSET, offset);
        Preconditions.checkArgument(offset < this.routerKeys.length, INVALIDOFFSET, offset, length);

        final int newLength = length - 1;
        if (newLength == 0) {
            Preconditions.checkArgument(emptyArray.length == 0);
            return emptyArray;
        }

        final T[] ret = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), newLength);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < newLength) {
            System.arraycopy(oldArray, offset + 1, ret, offset, newLength - offset);
        }

        return ret;
    }

    boolean isEmpty() {
        return this.size() == 0;
    }
}