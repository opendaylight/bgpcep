/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

/**
 * Abstract base class for implementing Prefix classes. This class correctly
 * implements all required methods, so to implement a Prefix class, you
 * only need to subclass it and publish an appropriate contructor.
 *
 * @param <T> template parameter reference to subclass
 */
public abstract class AbstractPrefix<T extends NetworkAddress<T>> implements Comparable<AbstractPrefix<T>>, Prefix<T> {

	private static final long serialVersionUID = -384546010175152481L;

	private final T address;
	private final int length;

	/**
	 * Create a new prefix object.
	 *
	 * @param address Base network address
	 * @param length Length of significant part of address, in bits
	 */
	protected AbstractPrefix(final T address, final int length) {
		this.address = address.applyMask(length);
		this.length = length;
	}

	@Override
	public T getAddress() {
		return this.address;
	}

	@Override
	public int getLength() {
		return this.length;
	}

	@Override
	public int compareTo(final AbstractPrefix<T> p) {
		final int i = this.address.compareTo(p.getAddress());
		if (i != 0)
			return i;
		if (this.length < p.getLength())
			return -1;
		if (this.length > p.getLength())
			return 1;
		return 0;
	}

	@Override
	public boolean contains(final NetworkAddress<?> address) {
		/*
		 * Fastest way of checking this is to apply this prefixe's
		 * mask and check for equality.
		 */
		final NetworkAddress<?> masked = address.applyMask(this.length);
		return this.address.equals(masked);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Returned string will be in the CIDR format.
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(this.address.toString());
		sb.append('/');
		sb.append(this.length);
		return sb.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final AbstractPrefix<?> other = (AbstractPrefix<?>) obj;
		if (this.address == null) {
			if (other.address != null)
				return false;
		} else if (!this.address.equals(other.address))
			return false;
		if (this.length != other.length)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.address == null) ? 0 : this.address.hashCode());
		result = prime * result + this.length;
		return result;
	}
}

