/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OperandsTest {

    @Test
    public void testParseLength() {
        // 00-00-0000 = 1
        assertEquals(1, AbstractOperandParser.parseLength((byte) 0x00));
        // 00-01-0000 = 2
        assertEquals(2, AbstractOperandParser.parseLength((byte) 16));
        // 00-10-0000 = 4
        assertEquals(4, AbstractOperandParser.parseLength((byte) 32));
        // 00-11-0000 = 8
        assertEquals(8, AbstractOperandParser.parseLength((byte) 48));
    }
}
