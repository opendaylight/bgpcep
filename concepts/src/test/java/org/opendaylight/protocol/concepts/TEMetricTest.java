/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TEMetricTest {

	@Test
	public void testOverflows() {
		try {
			new TEMetric(-2);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
		try {
			new TEMetric(4294967296L);
			fail("Constructor successful unexpectedly");
		} catch(IllegalArgumentException e) {}
	}

	@Test
	public void testGetValue() {
		TEMetric metric = new TEMetric(951357);
		assertEquals(951357, metric.getValue());
	}

	@Test
	public void testEqualsObject() {
		TEMetric metric1 = new TEMetric(159357);
		TEMetric metric2 = new TEMetric(159357);
		TEMetric metric3 = new TEMetric(258456);
		TEMetric metric4 = metric3;

		assertEquals(metric1, metric2);
		assertEquals(metric1.hashCode(), metric2.hashCode());
		assertEquals(metric3, metric4);
		assertNotNull(metric1);
		assertThat(metric1, not(equalTo(new Object())));
		assertThat(metric1, not(equalTo(metric3)));
		assertThat(metric1.hashCode(), not(equalTo(metric3.hashCode())));
	}
}

