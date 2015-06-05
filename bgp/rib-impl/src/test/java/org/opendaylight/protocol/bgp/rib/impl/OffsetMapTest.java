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

public class OffsetMapTest {

    private final String LOCAL_ADDRESS = "127.0.0.1";
    private final int LOCAL_ADDRESS_DECIMAL = 0x7f000001;
    private final Integer[] TESTED_VALUES = { 1, 2, 3, 4, 5, 6, 7 };
    private final int EXPECTED_ROUTER_OFFSET = 0;
    private final int EXPECTED_VALUE = 1;
    private final int CHANGED_VALUE = 111;

    private final int HOW_MANY = 3;

    @Test
    public void testAllMethods() {
        final OffsetMap offsetMap = OffsetMap.EMPTY.with(RouterIds.routerIdForAddress(this.LOCAL_ADDRESS));
        assertEquals(this.EXPECTED_ROUTER_OFFSET, offsetMap.offsetOf(RouterIds.routerIdForAddress(this.LOCAL_ADDRESS)));
        assertEquals(this.LOCAL_ADDRESS_DECIMAL, offsetMap.getRouterId(this.EXPECTED_ROUTER_OFFSET).intValue());

        assertEquals(this.EXPECTED_VALUE, (int)offsetMap.getValue(this.TESTED_VALUES, this.EXPECTED_ROUTER_OFFSET));
        offsetMap.setValue(this.TESTED_VALUES, this.EXPECTED_ROUTER_OFFSET, this.CHANGED_VALUE);
        assertEquals(this.CHANGED_VALUE, (int)offsetMap.getValue(this.TESTED_VALUES, this.EXPECTED_ROUTER_OFFSET));

        final OffsetMap emptyMap = OffsetMap.EMPTY.with(RouterIds.routerIdForAddress(this.LOCAL_ADDRESS));
        final Integer[] new_values = emptyMap.expand(offsetMap, this.TESTED_VALUES, this.HOW_MANY);

        assertEquals(this.HOW_MANY + offsetMap.size(), emptyMap.size());
        assertEquals(this.HOW_MANY + offsetMap.size(), new_values.length);
    }
}
