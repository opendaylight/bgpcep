/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.Arrays;

import org.opendaylight.protocol.concepts.Identifier;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Utility class for implementing identifiers. Subclasses need to provide
 * a byte[] representation of the encapsulated identifier which is unique
 * in its domain. This class then implements the required interfaces.
 *
 * @param <T> template parameter reference to subclass
 */
public abstract class AbstractIdentifier<T extends AbstractIdentifier<?>> implements Comparable<T>, Identifier {
	private static final long serialVersionUID = 7024836009610553163L;

	/**
	 * Get raw identifier bytes. Override this method to provide the base
	 * class with a raw representation of your identifier. This is then
	 * used in calculations.
	 *
	 * @return Bytearray representation of the identifier
	 */
	abstract protected byte[] getBytes();

	abstract protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper);

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.getBytes());
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;

		if (this.getClass().equals(o.getClass())
				&& o instanceof AbstractIdentifier<?>)
			return Arrays.equals(this.getBytes(),
					((AbstractIdentifier<?>) o).getBytes());
		return false;
	}

	@Override
	public int compareTo(final T id) {
		boolean assignable = this.getClass().isAssignableFrom(id.getClass());
		if (assignable == false) {
			throw new IllegalArgumentException("Object " + id + " is not assignable to " + getClass());
		}

		final byte[] myb = this.getBytes();
		final byte[] idb = id.getBytes();

		for (int i = 0; i < idb.length && i < myb.length; ++i) {
			if (myb[i] != idb[i])
				return myb[i] - idb[i];
		}

		if (idb.length != myb.length)
			return myb.length - idb.length;

		return 0;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}
}

