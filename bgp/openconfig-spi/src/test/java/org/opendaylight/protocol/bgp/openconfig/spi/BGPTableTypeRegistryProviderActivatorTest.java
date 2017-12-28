/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public class BGPTableTypeRegistryProviderActivatorTest {

    @Test
    public void testBGPTableTypeRegistryProviderActivator() {
        final AbstractBGPTableTypeRegistryProviderActivator activator =
            new AbstractBGPTableTypeRegistryProviderActivator() {
                @Override
                protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
                        final BGPTableTypeRegistryProvider provider) {
                    return Collections.singletonList(provider.registerBGPTableType(Ipv4AddressFamily.class,
                            UnicastSubsequentAddressFamily.class, IPV4UNICAST.class));
                }
            };

        final SimpleBGPTableTypeRegistryProvider provider = new SimpleBGPTableTypeRegistryProvider();
        activator.startBGPTableTypeRegistryProvider(provider);
        assertTrue(provider.getTableType(IPV4UNICAST.class).isPresent());
        activator.stopBGPTableTypeRegistryProvider();
        Assert.assertFalse(provider.getTableType(IPV4UNICAST.class).isPresent());
        activator.close();
    }

}
