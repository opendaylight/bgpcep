/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.impl.OffsetMap;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;

public class OffsetMapTest {

    private final String LOCAL_ADDRESS = "127.0.0.1";
    private final int LOCAL_ADDRESS_DECIMAL = 0x7f000001;
    private final Integer[] TESTED_VALUES = {1, 2, 3, 4, 5, 6, 7};
    private final Integer[] TESTED_VALUES_REMOVE = {2, 3, 4, 5, 6, 7};
    private final int EXPECTED_ROUTER_OFFSET = 0;
    private final int EXPECTED_VALUE = 1;
    private final int CHANGED_VALUE = 111;

    @Test
    public void testAllMethods() {
        final OffsetMap<UnsignedInteger> offsetMap = new OffsetMap<>(UnsignedInteger.class);
        final OffsetMap<UnsignedInteger> newOffsets = offsetMap.with(RouterIds.routerIdForAddress(LOCAL_ADDRESS));
        assertEquals(EXPECTED_ROUTER_OFFSET, newOffsets.offsetOf(RouterIds.routerIdForAddress(LOCAL_ADDRESS)));
        assertEquals(LOCAL_ADDRESS_DECIMAL, newOffsets.getRouterKey(EXPECTED_ROUTER_OFFSET).intValue());

        assertEquals(EXPECTED_VALUE, (int) newOffsets.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
        newOffsets.setValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET, CHANGED_VALUE);
        assertEquals(CHANGED_VALUE, (int) newOffsets.getValue(TESTED_VALUES, EXPECTED_ROUTER_OFFSET));
        assertArrayEquals(TESTED_VALUES_REMOVE, newOffsets.removeValue(TESTED_VALUES, 0));
        assertEquals(0, newOffsets.without(RouterIds.routerIdForAddress(LOCAL_ADDRESS)).size());
    }
}
