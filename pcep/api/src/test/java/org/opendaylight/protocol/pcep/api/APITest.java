/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;

public class APITest {

	@Test
	public void testDeserializerException() {
		final PCEPDeserializerException e = new PCEPDeserializerException("Some error message.");
		assertEquals("Some error message.", e.getErrorMessage());

		final PCEPDeserializerException e1 = new PCEPDeserializerException(new IllegalArgumentException(), "Some error message.");
		assertEquals("Some error message.", e1.getErrorMessage());
		assertTrue(e1.getCause() instanceof IllegalArgumentException);
	}
}
