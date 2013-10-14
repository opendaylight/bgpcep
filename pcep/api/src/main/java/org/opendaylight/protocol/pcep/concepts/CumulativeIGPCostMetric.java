/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.AbstractMetric;

/**
 * Cumulative IGP cost metric class
 */
public class CumulativeIGPCostMetric extends AbstractMetric<CumulativeIGPCostMetric> {

	private static final long serialVersionUID = 3935025327997428991L;

	/**
	 * Construct a new Cumulative IGP cost metric object.
	 * 
	 * @param value
	 *            Metric value
	 * @throws IllegalArgumentException
	 *             ex when value is outside of allowed range (0-4294967295)
	 */
	public CumulativeIGPCostMetric(final long value) {
		super(value);
		if (value < 0 || value > 4294967295L)
			throw new IllegalArgumentException("Invalid cumulative IGP cost metric value");
	}
}
