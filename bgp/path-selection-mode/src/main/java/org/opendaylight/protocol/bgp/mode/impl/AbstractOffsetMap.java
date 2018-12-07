/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import org.opendaylight.yangtools.concepts.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A map maintaining a set of values in an external array corresponding to a set of keys. This class is expected to be
 * used as a template, i.e. users subclass it to a concrete map and use exclusively that class to access the
 * functionality.
 */
@Beta
public abstract class AbstractOffsetMap<K extends Immutable & Comparable<K>, T extends AbstractOffsetMap<K, T>> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractOffsetMap.class);
    private static final String INVALIDOFFSET = "Invalid offset %s for %s router IDs";

    private final K[] keys;

    protected AbstractOffsetMap(final K[] emptyKeys, final Comparator<K> comparator, final ImmutableSet<K> routerIds) {
        final K[] array = routerIds.toArray(emptyKeys);
        Arrays.sort(array, comparator);
        this.keys = array;
    }

    public final K getKey(final int offset) {
        return this.keys[offset];
    }

    public final int offsetOf(final K key) {
        return Arrays.binarySearch(keys, key, comparator());
    }

    public final boolean isEmpty() {
        return keys.length == 0;
    }

    public final int size() {
        return keys.length;
    }

    public final T with(final K key) {
        // TODO: we could make this faster if we had an array-backed Set and requiring
        //       the caller to give us the result of offsetOf() -- as that indicates
        //       where to insert the new routerId while maintaining the sorted nature
        //       of the array
        final Builder<K> builder = ImmutableSet.builderWithExpectedSize(size() + 1);
        builder.add(keys);
        builder.add(key);
        return instanceForKeys(builder.build());
    }

    public final T without(final K key) {
        final ImmutableSet<K> set;
        final int index = indexOf(key);
        if (index < 0) {
            LOG.trace("Router key {} not found", key);
            set = ImmutableSet.of();
        } else {
            set = ImmutableSet.copyOf(removeValue(keys, index, emptyKeys()));
        }
        return instanceForKeys(set);
    }

    public final <C> C getValue(final C[] array, final int offset) {
        checkAccessOffest(offset);
        return array[offset];
    }

    public final <C> void setValue(final C[] array, final int offset, final C value) {
        checkAccessOffest(offset);
        array[offset] = value;
    }

    public final <C> C[] expand(final T oldOffsets, final C[] oldArray, final int offset) {
        @SuppressWarnings("unchecked")
        final C[] ret = (C[]) Array.newInstance(oldArray.getClass().getComponentType(), keys.length);

        System.arraycopy(oldArray, 0, ret, 0, offset);
        System.arraycopy(oldArray, offset, ret, offset + 1, oldOffsets.size() - offset);
        return ret;
    }

    public final <C> C[] removeValue(final C[] oldArray, final int offset, final C[] emptyArray) {
        checkNegativeOffset(offset);
        final int length = oldArray.length;
        checkArgument(offset < keys.length, INVALIDOFFSET, offset, length);

        final int newLength = length - 1;
        if (newLength == 0) {
            checkArgument(emptyArray.length == 0);
            return emptyArray;
        }

        @SuppressWarnings("unchecked")
        final C[] ret = (C[]) Array.newInstance(oldArray.getClass().getComponentType(), newLength);
        System.arraycopy(oldArray, 0, ret, 0, offset);
        if (offset < newLength) {
            System.arraycopy(oldArray, offset + 1, ret, offset, newLength - offset);
        }

        return ret;
    }

    protected abstract Comparator<K> comparator();

    protected abstract K[] emptyKeys();

    protected abstract T instanceForKeys(ImmutableSet<K> newKeys);

    private int indexOf(final K key) {
        for (int i = 0; i < keys.length; i++) {
            if (key.equals(keys[i])) {
                return i;
            }
        }
        return -1;
    }

    private void checkAccessOffest(final int offset) {
        checkNegativeOffset(offset);
        checkArgument(offset < keys.length, INVALIDOFFSET, offset, keys.length);
    }

    private static void checkNegativeOffset(final int offset) {
        checkArgument(offset >= 0, "Invalid negative offset %s", offset);
    }
}
