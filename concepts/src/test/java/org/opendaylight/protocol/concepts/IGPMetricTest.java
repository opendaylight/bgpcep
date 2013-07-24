/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IGPMetricTest {

	@Test
	public void testOverflows() {
		try {
			new IGPMetric(-2);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
		try {
			new IGPMetric(16777216);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
	}

	@Test
	public void testGetValue() {
		IGPMetric metric = new IGPMetric(951357);
		assertEquals(951357, metric.getValue());
	}

	@Test
	public void testEqualsObject() {
		IGPMetric metric1 = new IGPMetric(159357);
		IGPMetric metric2 = new IGPMetric(159357);
		IGPMetric metric3 = new IGPMetric(258456);
		IGPMetric metric4 = metric3;

		assertEquals(metric1, metric2);
		assertEquals(metric1.hashCode(), metric2.hashCode());
		assertEquals(metric3, metric4);
		assertNotNull(metric1);
		assertThat(metric1, not(new Object()));
		assertThat(metric1, not(metric3));
		assertThat(metric1.hashCode(), not(metric3.hashCode()));
	}

}
