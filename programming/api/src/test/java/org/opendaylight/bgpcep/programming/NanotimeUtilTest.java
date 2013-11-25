/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

public class NanotimeUtilTest {

	@Test
	public void testCurrentTime() {
		assertTrue(NanotimeUtil.currentTime().getValue().divide(BigInteger.valueOf(1000000)).subtract(
				BigInteger.valueOf(System.currentTimeMillis())).shortValue() <= 0);
	}

	@Test
	public void testNanoTime() {
		System.out.println(NanotimeUtil.currentNanoTime());
	}
}
