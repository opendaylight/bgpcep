/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;

class TableTypeTest {

    @Test
    void testTableTypes() {
        final BgpTableType tt1 = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
                MplsLabeledVpnSubsequentAddressFamily.VALUE);
        final BgpTableType tt2 = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
                MplsLabeledVpnSubsequentAddressFamily.VALUE);

        final var afiEx = assertThrows(NullPointerException.class,
            () -> new BgpTableTypeImpl(null, MplsLabeledVpnSubsequentAddressFamily.VALUE));
        assertEquals("Address family may not be null", afiEx.getMessage());

        final var safiEx = assertThrows(NullPointerException.class,
            () -> new BgpTableTypeImpl(Ipv6AddressFamily.VALUE, null));
        assertEquals("Subsequent address family may not be null", safiEx.getMessage());

        assertNotEquals(tt1, tt2);
        assertNotSame(tt1.hashCode(), tt2.hashCode());
        assertEquals(tt1.toString(), tt1.toString());
        assertNotSame(tt1.getAfi(), tt2.getAfi());
        assertEquals(tt1.getSafi(), tt2.getSafi());
    }
}
