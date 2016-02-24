/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OffsetMapTest {

    private final String KEY = "testKey";
    private final Integer[] TESTED_VALUES = { 1, 2, 3, 4, 5, 6, 7 };
    private final Integer[] TESTED_VALUES_REMOVE = {2, 3, 4, 5, 6, 7 };
    private final int EXPECTED_ROUTER_OFFSET = 0;
    private final int EXPECTED_VALUE = 1;
    private final int CHANGED_VALUE = 111;

    @Test
    public void testAllMethods() {
        final OffsetMap offsetMap = OffsetMap.EMPTY.with(KEY);
        assertEquals(EXPECTED_ROUTER_OFFSET, offsetMap.offsetOf(KEY));
        assertEquals(KEY, offsetMap.getRouterKey(EXPECTED_ROUTER_OFFSET));

        assertEquals(EXPECTED_VALUE, (int) offsetMap.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
        offsetMap.setValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET, CHANGED_VALUE);
        assertEquals(CHANGED_VALUE, (int) offsetMap.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
        assertArrayEquals(TESTED_VALUES_REMOVE, offsetMap.removeValue(TESTED_VALUES, 0));
        assertEquals(0, offsetMap.without(KEY).size());
    }
}
