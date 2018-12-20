/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.yangtools.concepts.Registration;

public class MultiRegistryTest {

    @Test
    public void testMultiRegistry() {
        final MultiRegistry<Object, Integer> registry = new MultiRegistry<>();
        final String first = "first";
        final String second = "second";
        final String third = "third";

        final Registration a = registry.register(first, 1);
        registry.register(second, 2);
        registry.register(third, 3);

        assertEquals(Integer.valueOf(1), registry.get("first"));
        assertEquals(Integer.valueOf(2), registry.get("second"));
        assertEquals(Integer.valueOf(3), registry.get("third"));

        registry.register(second, 22);

        assertEquals(Integer.valueOf(22), registry.get("second"));

        registry.register('c', 5);

        assertEquals(Integer.valueOf(5), registry.get('c'));

        a.close();

        assertNull(registry.get("first"));
    }

    @Test
    public void testDifferentNumbers() {
        final MultiRegistry<Object, Number> registry = new MultiRegistry<>();
        final String first = "first";

        registry.register(first, 1);
        assertEquals(1, registry.get("first"));

        registry.register(first, (short) 1);
        assertEquals(1, registry.get("first"));

        registry.register(first, (short) 2);
        assertEquals(1, registry.get("first"));
    }

    @Test
    public void testDifferentClasses() {
        final MultiRegistry<Object, Object> registry = new MultiRegistry<>();
        final String first = "first";
        final String second = "second";

        registry.register(first, 1);
        assertEquals(1, registry.get("first"));

        registry.register(first, '1');
        assertEquals(1, registry.get("first"));

        registry.register(second, '2');
        assertEquals('2', registry.get("second"));

        registry.register(second, 2);
        assertEquals('2', registry.get("second"));

        registry.register(second, new Object());
        assertEquals('2', registry.get("second"));
    }
}
