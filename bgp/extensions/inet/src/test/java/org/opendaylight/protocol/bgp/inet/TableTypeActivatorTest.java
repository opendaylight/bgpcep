/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(IPV4UNICAST.class, registry.getAfiSafiType(IPV4));
        assertEquals(IPV6UNICAST.class, registry.getAfiSafiType(IPV6));
        assertEquals(IPV4, registry.getTableType(IPV4UNICAST.class));
        assertEquals(IPV6, registry.getTableType(IPV6UNICAST.class));
    }
}
