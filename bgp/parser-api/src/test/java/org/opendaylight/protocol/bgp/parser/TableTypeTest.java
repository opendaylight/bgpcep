/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

public class TableTypeTest {

    @Test
    public void testTableTypes() {
        final BgpTableType tt1 = new BgpTableTypeImpl(Ipv4AddressFamily.class,
                MplsLabeledVpnSubsequentAddressFamily.class);
        final BgpTableType tt2 = new BgpTableTypeImpl(Ipv6AddressFamily.class,
                MplsLabeledVpnSubsequentAddressFamily.class);

        try {
            new BgpTableTypeImpl(null, MplsLabeledVpnSubsequentAddressFamily.class);
            fail("Null AFI!");
        } catch (final NullPointerException e) {
            assertEquals("Address family may not be null", e.getMessage());
        }

        try {
            new BgpTableTypeImpl(Ipv6AddressFamily.class, null);
            fail("Null SAFI!");
        } catch (final NullPointerException e) {
            assertEquals("Subsequent address family may not be null", e.getMessage());
        }

        assertFalse(tt1.equals(tt2));
        assertNotSame(tt1.hashCode(), tt2.hashCode());
        assertEquals(tt1.toString(), tt1.toString());
        assertNotSame(tt1.getAfi(), tt2.getAfi());
        assertEquals(tt1.getSafi(), tt2.getSafi());
    }
}
