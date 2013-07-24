/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Extension of the TypesafeContainer which supports the notion of a default
 * key.
 *
 * @param <S> Supertype of all objects stored in this container
 */
public class DefaultingTypesafeContainer<S> extends TypesafeContainer<S> {
	private static final long serialVersionUID = 140170613440644179L;
	private Class<? extends S> defaultKey = null;

	/**
	 * Returns the default entry.
	 *
	 * @return Default entry, or null if no default was set
	 */
	public S getDefaultEntry() {
		if (defaultKey == null) {
			return null;
		}
		return defaultKey.cast(getEntry(defaultKey));
	}

	/**
	 * Sets or resets the default entry.
	 *
	 * @param entry Default entry value. Use null to reset the container
	 *        to no default entry.
	 */
	public void setDefaultEntry(final S entry) {
		if (defaultKey != null) {
			removeEntry(defaultKey);
			defaultKey = null;
		}
		if (entry != null) {
			/*
			 * This is rather obvious: entry is required to be a subclass
			 * of T. This implies that its class conforms to
			 * Class<? extends T>. For some reason, the compiler is not
			 * smart enough to know this.
			 */
			@SuppressWarnings("unchecked")
			final Class<? extends S> c = (Class<? extends S>) entry.getClass();
			defaultKey = c;

			setEntry(defaultKey, entry);
		}
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("defaultKey", this.defaultKey);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 7 + (defaultKey != null ? defaultKey.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DefaultingTypesafeContainer == false) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (super.equals(obj)==false) {
			return false;
		}
		Object thatDefaultKey = ((DefaultingTypesafeContainer<?>)obj).defaultKey;
		if (defaultKey == thatDefaultKey) {
            return true;
        }
        if ((defaultKey == null) != (thatDefaultKey == null)) {
            return false;
        }
        return thatDefaultKey.equals(thatDefaultKey);
	}

}
