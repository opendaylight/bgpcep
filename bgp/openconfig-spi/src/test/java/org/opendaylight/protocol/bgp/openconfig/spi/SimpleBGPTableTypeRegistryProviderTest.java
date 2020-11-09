/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;

public class SimpleBGPTableTypeRegistryProviderTest {
    private final BGPTableTypeRegistryProvider provider = new SimpleBGPTableTypeRegistryProvider();
    private Registration registration;

    @Before
    public void setUp() {
        registration = provider.registerBGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
            IPV4UNICAST.class);
    }

    @Test
    public void testBGPTableTypeRegistryProvider() {
        assertEquals(Optional.of(IPV4UNICAST.class), provider.getAfiSafiType(
            new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)));
        assertEquals(Optional.empty(), provider.getAfiSafiType(
            new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class)));

        assertTrue(provider.getTableType(IPV4UNICAST.class).isPresent());
        assertEquals(Optional.empty(),provider.getTableType(IPV6UNICAST.class));

        registration.close();
        assertEquals(Optional.empty(), provider.getTableType(IPV4UNICAST.class));
    }

    @Test
    public void testDuplicatedRegistration() {
        assertThrows(IllegalStateException.class, () -> provider.registerBGPTableType(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class, IPV4UNICAST.class));
    }
}
