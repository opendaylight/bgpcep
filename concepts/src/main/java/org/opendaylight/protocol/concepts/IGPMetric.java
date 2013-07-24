/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

/**
 * Interior Gateway Protocol metric class.
 */
public final class IGPMetric extends AbstractMetric<IGPMetric> {
	private static final long serialVersionUID = -401875021705133799L;

	/**
	 * Construct a new IGP metric object.
	 *
	 * @param value Metric value
	 * @throws IllegalArgumentException ex when value is outside of allowed
	 *         range (0-16777215)
	 */
	public IGPMetric(final long value) {
		super(value);
		if (value < 0 || value > 16777215)
			throw new IllegalArgumentException("Invalid IGP metric value");
	}
}

