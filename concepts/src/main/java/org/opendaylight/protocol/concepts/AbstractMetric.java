/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

/**
 * Abstract helper class for metrics which are based on integral values,
 * with numberically lower numbers being considered better candidates.
 *
 * @param <T> template reference to subclass
 */
public abstract class AbstractMetric<T extends AbstractMetric<?>> implements Metric<T> {
	private static final long serialVersionUID = 4404624006216930524L;
	private final long value;

	protected AbstractMetric(final long value) {
		this.value = value;
	}

	/**
	 * Get the metric value.
	 *
	 * @return Metric value
	 */
	public long getValue() {
		return value;
	}

	@Override
	public int compareTo(final T other) {
		if (value < other.getValue())
			return -1;
		if (value > other.getValue())
			return 1;
		return 0;
	}

	@Override
	public final int hashCode() {
		return (int)value;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj.getClass() != this.getClass())
			return false;
		return value == ((AbstractMetric<?>)obj).getValue();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}

