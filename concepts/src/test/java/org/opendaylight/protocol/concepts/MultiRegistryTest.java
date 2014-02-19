/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultiRegistryTest {

	@Test
	public void testMultiRegistry() {
		MultiRegistry<Object, Integer> registry = new MultiRegistry<>();
		String first = "first";
		String second = "second";
		String third = "third";

		registry.register(first, 1);
		registry.register(second, 2);
		registry.register(third, 3);

		assertEquals(Integer.valueOf(1), registry.get("first"));
		assertEquals(Integer.valueOf(2), registry.get("second"));
		assertEquals(Integer.valueOf(3), registry.get("third"));

		registry.register(second, 22);

		assertEquals(Integer.valueOf(22), registry.get("second"));

		registry.register(Character.valueOf('c'), 5);

		assertEquals(Integer.valueOf(5), registry.get('c'));
	}

}
