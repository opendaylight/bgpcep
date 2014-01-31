/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

@Deprecated
public class RemoveOnlySet<E> implements Set<E> {
	private final Set<E> set;

	public RemoveOnlySet(final Set<E> set) {
		this.set = set;
	}

	public static <T> RemoveOnlySet<T> wrap(final Set<T> set) {
		return new RemoveOnlySet<T>(set);
	}

	@Override
	public boolean add(final E e) {
		throw new UnsupportedOperationException("Set does not accept additions");
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		throw new UnsupportedOperationException("Set does not accept additions");
	}

	@Override
	public void clear() {
		this.set.clear();
	}

	@Override
	public boolean contains(final Object o) {
		return this.set.contains(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.set.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return this.set.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.set.iterator();
	}

	@Override
	public boolean remove(final Object o) {
		return this.set.remove(o);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return this.set.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return this.set.retainAll(c);
	}

	@Override
	public int size() {
		return this.set.size();
	}

	@Override
	public Object[] toArray() {
		return this.set.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return this.set.toArray(a);
	}
}
