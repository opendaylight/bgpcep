/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OffsetMapTests {

    private final String LOCAL_ADDRESS = "127.0.0.1";
    private final int LOCAL_ADDRESS_DECIMAL = 0x7f000001;
    private final Integer[] TESTED_VALUES = { 1, 2, 3, 4, 5, 6, 7 };
    private final int EXPECTED_ROUTER_OFFSET = 0;
    private final int EXPECTED_VALUE = 1;
    private final int CHANGED_VALUE = 111;

    @Test
    public void testAllMethods() {
        final OffsetMap offsetMap = OffsetMap.EMPTY.with(RouterIds.routerIdForAddress(LOCAL_ADDRESS));
        assertEquals(EXPECTED_ROUTER_OFFSET, offsetMap.offsetOf(RouterIds.routerIdForAddress(LOCAL_ADDRESS)));
        assertEquals(LOCAL_ADDRESS_DECIMAL, offsetMap.getRouterId(EXPECTED_ROUTER_OFFSET).intValue());

        assertEquals(EXPECTED_VALUE, (int)offsetMap.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
        offsetMap.setValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET, CHANGED_VALUE);
        assertEquals(CHANGED_VALUE, (int)offsetMap.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
    }
}
