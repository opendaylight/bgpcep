/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import java.io.Serializable;

/**
 * Interface defining the concept of a metric. Metrics are something which
 * defines preference of an object when used in a certain context. In that
 * sense we define total ordering of metrics in the same class.
 *
 * @param <T> subtype of Metric
 */
public interface Metric<T extends Metric<?>> extends Comparable<T>, Serializable {
	/**
	 * {@inheritDoc}
	 *
	 * Metrics which compare as 'lower' using this method are to be
	 * considered better candidates than the other metric.
	 */
	@Override
	public int compareTo(T o);

}

