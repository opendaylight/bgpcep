/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.DefaultBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final BgpTableType IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.class,
            UnicastSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        try (var registry = new DefaultBGPTableTypeRegistryProvider(new TableTypeActivator())) {
            assertEquals(Optional.of(IPV4UNICAST.class), registry.getAfiSafiType(IPV4));
            assertEquals(Optional.of(IPV6UNICAST.class), registry.getAfiSafiType(IPV6));
            assertEquals(Optional.of(IPV4), registry.getTableType(IPV4UNICAST.class));
            assertEquals(Optional.of(IPV6), registry.getTableType(IPV6UNICAST.class));
        }
    }
}
