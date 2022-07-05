/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

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
    private final Registration registration = provider.registerBGPTableType(
        Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE, IPV4UNICAST.VALUE);

    @Test
    public void testBGPTableTypeRegistryProvider() {
        assertEquals(IPV4UNICAST.class, provider.getAfiSafiType(
            new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)));
        assertNull(provider.getAfiSafiType(
            new BgpTableTypeImpl(Ipv6AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)));

        assertNotNull(provider.getTableType(IPV4UNICAST.VALUE));
        assertNull(provider.getTableType(IPV6UNICAST.VALUE));

        registration.close();
        assertNull(provider.getTableType(IPV4UNICAST.VALUE));
    }

    @Test
    public void testDuplicatedRegistration() {
        assertThrows(IllegalStateException.class, () -> provider.registerBGPTableType(Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE, IPV4UNICAST.VALUE));
    }
}
