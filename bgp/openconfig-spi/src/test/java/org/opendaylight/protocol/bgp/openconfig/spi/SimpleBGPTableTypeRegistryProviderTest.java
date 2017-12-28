/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public class SimpleBGPTableTypeRegistryProviderTest {

    private BGPTableTypeRegistryProvider provider;
    private AbstractRegistration registration;

    @Before
    public void setUp() {
        this.provider = new SimpleBGPTableTypeRegistryProvider();
        this.registration = this.provider.registerBGPTableType(Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class, IPV4UNICAST.class);
    }

    @Test
    public void testBGPTableTypeRegistryProvider() {

        final Optional<Class<? extends AfiSafiType>> afiSafiType = this.provider.getAfiSafiType(
                new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        Assert.assertTrue(afiSafiType.isPresent());
        final Optional<Class<? extends AfiSafiType>> afiSafiType2 = this.provider.getAfiSafiType(
                new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        Assert.assertFalse(afiSafiType2.isPresent());

        final Optional<BgpTableType> tableType = this.provider.getTableType(IPV4UNICAST.class);
        Assert.assertTrue(tableType.isPresent());
        final Optional<BgpTableType> tableType2 = this.provider.getTableType(IPV6UNICAST.class);
        Assert.assertFalse(tableType2.isPresent());

        this.registration.close();
        final Optional<BgpTableType> tableType3 = this.provider.getTableType(IPV4UNICAST.class);
        Assert.assertFalse(tableType3.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicatedRegistration() {
        this.provider.registerBGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
                IPV4UNICAST.class);
    }

}
