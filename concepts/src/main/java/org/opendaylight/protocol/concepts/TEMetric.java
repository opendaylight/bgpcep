/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.Immutable;

/**
 * Traffic Engineering metric class.
 */
@Immutable
public final class TEMetric extends AbstractMetric<TEMetric> {
	private static final long serialVersionUID = -6843828299489991771L;

	/**
	 * Construct a new TE metric object.
	 *
	 * @param value Metric value
	 * @throws IllegalArgumentException ex when value is outside of allowed
	 *         range (0-4294967295)
	 */
	public TEMetric(final long value) {
		super(value);
		if (value < 0 || value > 4294967295L)
			throw new IllegalArgumentException("Invalid TE metric value");
	}
}

