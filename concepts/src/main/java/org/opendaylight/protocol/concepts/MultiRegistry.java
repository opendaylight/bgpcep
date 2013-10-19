/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

@ThreadSafe
public final class MultiRegistry<K, V> {
	private final ConcurrentMap<K, V> current = new ConcurrentHashMap<>();

	@GuardedBy("this")
	private final ListMultimap<K, V> candidates = ArrayListMultimap.create();

	public synchronized AbstractRegistration register(final K key, final V value) {
		// Put this value into candidates, then put it into the the current
		// map, if it does not have a key -- this is to prevent unnecessary
		// churn by replacing an already-present mapping.
		candidates.put(key, value);
		current.putIfAbsent(key, value);

		final Object lock = this;
		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				synchronized (lock) {
					// The delete sequencing is a bit more complex, as we want
					// to prevent churn and we do not want the user to see
					// the current map without a mapping as long as a candidate
					// exists. To achieve this, we remove the entry from
					// candidates and then check if the list of candidates for
					// this key has become empty. If it has, we just remove
					// the mapping. If there are candidates, then attempt to
					// replace it -- but only if it has pointed to the removed
					// value in the first place.
					candidates.remove(key, value);

					final List<V> values = candidates.get(key);
					if (values.isEmpty()) {
						current.remove(key);
					} else {
						current.replace(key, value, values.get(0));
					}
				}
			}
		};
	}

	public V get(final K key) {
		return current.get(key);
	}
}
