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

import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;

public class BGPTableTypeRegistryProviderActivatorTest {
    @Test
    public void testBGPTableTypeRegistryProviderActivator() {
        final BGPTableTypeRegistryProviderActivator activator =
            provider -> List.of(provider.registerBGPTableType(Ipv4AddressFamily.VALUE,
                UnicastSubsequentAddressFamily.VALUE, IPV4UNICAST.class));

        final SimpleBGPTableTypeRegistryProvider provider = new SimpleBGPTableTypeRegistryProvider();
        final List<Registration> regs = activator.startBGPTableTypeRegistryProvider(provider);
        assertNotNull(provider.getTableType(IPV4UNICAST.class));
        assertEquals(1, regs.size());

        regs.get(0).close();
        assertNull(provider.getTableType(IPV4UNICAST.class));
    }
}
