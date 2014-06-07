/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class ApiTest {

    @Test
    public void testBGPSessionPreferences() {
        final BGPSessionPreferences sp = new BGPSessionPreferences(new AsNumber(58L), (short) 5, null, null);
        assertNull(sp.getBgpId());
        assertEquals((short) 5, sp.getHoldTime());
        assertEquals(new AsNumber(58L), sp.getMyAs());
    }
}
