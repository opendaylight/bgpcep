/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatisticsUtilTest {

    @Test
    public void testGetCurrentTimestampInSeconds() {
        assertEquals(System.currentTimeMillis() / 1000, StatisticsUtil.getCurrentTimestampInSeconds());
    }

    @Test
    public void testFormatElapsedTime() {
        assertEquals("1:01:01:01", StatisticsUtil.formatElapsedTime(90061));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatElapsedTimeInvalidInput() {
        StatisticsUtil.formatElapsedTime(-1);
    }
}
