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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple {@link RibTableEntry} objects,
 * which share either contributors or consumers.
 *
 * We provide utility functions for producer/consumer synchronization
 * arrays. These arrays have twice the required capacity and track
 * the produced objects (at {@link #offsetOf(Ipv4Address)}) and objects
 * last seen by consumer (offset by the size of producers). While this
 * layout does not prevent cache-line bounces, it lowers their likelihood
 * as the number of contributors grows (and the array naturally spans
 * multiple cache-lines).
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

    <T> T getConsumed(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        return array[offset];
    }

    <T> void setConsumed(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        array[offset] = value;
    }

    <T> T getProduced(final T[] array, final int offset) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        return array[routerIds.length + offset];
    }

    <T> void setProduced(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        array[routerIds.length + offset] = value;
    }

    <T> T[] reformat(final OffsetMap oldOffsets, final T[] oldArray, final int offset) {
        @SuppressWarnings("unchecked")
        final T[] ret = (T[]) new Object[this.routerIds.length * 2];
        final int oldSize = oldOffsets.routerIds.length;

        System.arraycopy(oldArray, 0, ret, 0, offset);
        System.arraycopy(oldArray, offset, ret, offset + 1, oldSize);
        System.arraycopy(oldArray, offset + oldSize, ret, oldSize + offset + 2, oldSize - offset);

        return ret;
    }
}
