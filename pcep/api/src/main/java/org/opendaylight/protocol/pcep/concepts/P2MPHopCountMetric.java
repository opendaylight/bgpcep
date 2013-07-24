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
 * P2MP hop count metric class
 */
public class P2MPHopCountMetric extends AbstractMetric<P2MPHopCountMetric> {

	private static final long serialVersionUID = 3935025327997428991L;

	/**
	 * Construct a new P2MP hop count metric object.
	 * 
	 * @param value
	 *            Metric value
	 * @throws IllegalArgumentException
	 *             ex when value is outside of allowed range (0-4294967295)
	 */
	public P2MPHopCountMetric(final long value) {
		super(value);
		if (value < 0 || value > 4294967295L)
			throw new IllegalArgumentException("Invalid P2MP hop count metric value");
	}
}
