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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * A map of Router identifier to an offset. Used to maintain a simple
 * offset-based lookup across multiple {@link RibTableEntry} objects,
 * which share either contributors or consumers.
 */
final class OffsetMap {
    static final OffsetMap EMPTY = new OffsetMap(Collections.<Ipv4Address>emptySet());
    private static final LoadingCache<Set<Ipv4Address>, OffsetMap> OFFSETMAPS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Set<Ipv4Address>, OffsetMap>() {
        @Override
        public OffsetMap load(final Set<Ipv4Address> key) throws Exception {
            return new OffsetMap(key);
        }
    });
    private static final Comparator<Ipv4Address> IPV4_COMPARATOR = new Comparator<Ipv4Address>() {
        @Override
        public int compare(final Ipv4Address o1, final Ipv4Address o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };
    private final Ipv4Address[] routerIds;

    private OffsetMap(final Set<Ipv4Address> routerIds) {
        final Ipv4Address[] array = routerIds.toArray(new Ipv4Address[0]);
        Arrays.sort(array, IPV4_COMPARATOR);
        this.routerIds = array;
    }

    @SuppressWarnings("unchecked")
    <T> T[] allocateArray() {
        return (T[]) new Object[this.routerIds.length * 2];
    }

    <T> T lookupConsumed(final T[] array, final Ipv4Address routerId) {
        return array[offsetOf(routerId)];
    }

    <T> void setConsumed(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        array[offset] = value;
    }

    <T> T lookupProduced(final T[] array, final Ipv4Address routerId) {
        return array[array.length - offsetOf(routerId)];
    }

    <T> void setProduced(final T[] array, final int offset, final T value) {
        Preconditions.checkArgument(offset >= 0, "Invalid negative offset {}", offset);
        array[array.length - offset] = value;
    }

    Ipv4Address routerIdAt(final int offset) {
        return this.routerIds[offset];
    }

    int size() {
        return this.routerIds.length;
    }

    int offsetOf(final Ipv4Address routerId) {
        return Arrays.binarySearch(this.routerIds, routerId, IPV4_COMPARATOR);
    }

    OffsetMap with(final Ipv4Address routerId) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<Ipv4Address> b = ImmutableSet.builder();
        b.add(this.routerIds);
        b.add(routerId);

        return OFFSETMAPS.getUnchecked(b.build());
    }
}
