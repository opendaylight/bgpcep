/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class IPv6NextHopTest {

    private Ipv6NextHop nextHopA;
    private Ipv6NextHop nextHopB;

    @Before
    public void init() {
        this.nextHopA = new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:7331")).build();
        this.nextHopB = new Ipv6NextHopBuilder().setGlobal(new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:7331")).setLinkLocal(
                new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:0000")).build();
    }

    @Test
    public void testGetGlobal() {
        final Ipv6Address globalTestAddress = new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:7331");

        assertEquals(this.nextHopA.getGlobal(), globalTestAddress);
        assertEquals(this.nextHopB.getGlobal(), globalTestAddress);
    }

    @Test
    public void testGetLinkLocal() {
        final Ipv6Address localTestAddress = new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:0000");

        assertNull(this.nextHopA.getLinkLocal());
        assertEquals(this.nextHopB.getLinkLocal(), localTestAddress);
    }
}
