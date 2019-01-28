/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractBGPSessionNegotiator.AS_TRANS;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractBGPSessionNegotiator.openASNumber;

import org.junit.Test;

public class AbstractBGPSessionNegotiatorTest {
    @Test
    public void testOpenASNumber() {
        assertEquals(0, openASNumber(0));
        assertEquals(65535, openASNumber(65535));
        assertEquals(AS_TRANS, openASNumber(65536));
        assertEquals(AS_TRANS, openASNumber(Integer.MAX_VALUE));
        assertEquals(AS_TRANS, openASNumber(2147483648L));
        assertEquals(AS_TRANS, openASNumber(4294967295L));
    }
}
