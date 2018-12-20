/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry which allows multiple values for a particular key. One of those is considered the best and returned as the
 * representative.
 * When selecting the candidate, we evaluate the order of insertion, picking the value inserted first, but then we look
 * at all the other candidates and if there is one which is a subclass of the first one, we select that one.
 *
 * @param <K> key type
 * @param <V> value type
 */
@ThreadSafe
public final class MultiRegistry<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(MultiRegistry.class);
    private final ConcurrentMap<K, V> current = new ConcurrentHashMap<>();

    @GuardedBy("this")
    private final ListMultimap<K, V> candidates = ArrayListMultimap.create();

    @GuardedBy("this")
    private void updateCurrent(final K key) {
        final List<V> values = this.candidates.get(key);

        // Simple case: no candidates
        if (values.isEmpty()) {
            this.current.remove(key);
            return;
        }

        V best = values.get(0);
        for (V v : values) {
            final Class<?> vc = v.getClass();
            final Class<?> bc = best.getClass();
            if (bc.isAssignableFrom(vc)) {
                LOG.debug("{} is superclass of {}, preferring the latter", bc, vc);
                best = v;
            } else if (vc.isAssignableFrom(bc)) {
                LOG.debug("{} is subclass of {}, preferring the former", bc, vc);
            } else {
                LOG.debug("{} and {} are not related, keeping the former", bc, vc);
            }
        }

        LOG.debug("New best value {}", best);
        this.current.put(key, best);
    }

    public synchronized Registration register(final K key, final V value) {
        this.candidates.put(key, value);
        updateCurrent(key);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (MultiRegistry.this) {
                    MultiRegistry.this.candidates.remove(key, value);
                    updateCurrent(key);
                }
            }
        };
    }

    public V get(final K key) {
        return this.current.get(key);
    }

    public Iterable<V> getAllValues() {
        return Iterables.unmodifiableIterable(this.current.values());
    }
}
