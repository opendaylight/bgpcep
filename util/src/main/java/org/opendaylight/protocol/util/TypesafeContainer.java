/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Container to store different types of objects and retrieve them back in
 * a type-safe manner. this is a direct application of Joshua Bloch's typesafe
 * heterogeneous container pattern from Effective Java.
 *
 * @param <S> Supertype of all objects stored in this container
 */
public class TypesafeContainer<S> implements Serializable {
	private static final long serialVersionUID = 4369804407106485605L;
	private final Map<Class<? extends S>, Object> entries;

	/**
	 * Create a new container with default initial capacity
	 */
	public TypesafeContainer() {
		entries = new HashMap<Class<? extends S>, Object>();
	}

	/**
	 * Create a new container with specified initial capacity
	 *
	 * @param initialCapacity Desired initial capacity
	 */
	public TypesafeContainer(final int initialCapacity) {
		entries = new HashMap<Class<? extends S>, Object>(initialCapacity);
	}

	/**
	 * Get entry types currently present in the container.
	 *
	 * @return Entry types currently present in the contaier
	 */
	public Set<Class<? extends S>> getEntryTypes() {
		return entries.keySet();
	}

	/**
	 * Returns entry of a particular type
	 *
	 * @param clazz Entry type
	 * @return T Entry value, null if entry of specified type is not present
	 */
	public <T extends S> T getEntry(final Class<T> clazz) {
		return clazz.cast(entries.get(clazz));
	}

	/**
	 * Remove the entry for a particular type
	 *
	 * @param clazz Entry type, may not be null
	 */
	public void removeEntry(final Class<? extends S> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("Entry type may not be null");
		}
		entries.remove(clazz);
	}

	/**
	 * Sets an entry of particular type
	 *
	 * @param clazz Entry type
	 * @param entry Entry value, may not be null
	 */
	public void setEntry(final Class<? extends S> clazz, final S entry) {
		if (clazz == null) {
			throw new IllegalArgumentException("Entry type may not be null");
		}
		if (entry == null) {
			throw new IllegalArgumentException("Entry value may not be null");
		}

		entries.put(clazz, entry);
	}

	@Override
	public String toString(){
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("entries", this.entries);
		return toStringHelper;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TypesafeContainer == false) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return entries.equals(((TypesafeContainer<?>)obj).entries);
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

}

