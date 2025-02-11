/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StatisticsUtilTest {
    @Test
    void testGetCurrentTimestampInSeconds() {
        assertEquals(System.currentTimeMillis() / 1000, StatisticsUtil.getCurrentTimestampInSeconds());
    }

    @Test
    void testFormatElapsedTime() {
        assertEquals("1:01:01:01", StatisticsUtil.formatElapsedTime(90061));
    }

    @Test
    void testFormatElapsedTimeInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> StatisticsUtil.formatElapsedTime(-1));
    }
}
