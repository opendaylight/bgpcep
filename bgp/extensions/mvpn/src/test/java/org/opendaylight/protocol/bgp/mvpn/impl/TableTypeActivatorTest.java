/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.SimpleBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;

public final class TableTypeActivatorTest {

    private static final BgpTableType MVPN_IPV4 = new BgpTableTypeImpl(
            Ipv4AddressFamily.class, McastVpnSubsequentAddressFamily.class);
    private static final BgpTableType MVPN_IPV6 = new BgpTableTypeImpl(
            Ipv6AddressFamily.class, McastVpnSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        final TableTypeActivator tableTypeActivator = new TableTypeActivator();
        final SimpleBGPTableTypeRegistryProvider registry = new SimpleBGPTableTypeRegistryProvider();
        tableTypeActivator.startBGPTableTypeRegistryProvider(registry);

        final Optional<Class<? extends AfiSafiType>> afiSafiType4 = registry.getAfiSafiType(MVPN_IPV4);
        Assert.assertEquals(IPV4MCASTVPN.class, afiSafiType4.get());
        final Optional<Class<? extends AfiSafiType>> afiSafiType6 = registry.getAfiSafiType(MVPN_IPV6);
        Assert.assertEquals(IPV6MCASTVPN.class, afiSafiType6.get());

        final Optional<BgpTableType> tableType4 = registry.getTableType(IPV4MCASTVPN.class);
        Assert.assertEquals(MVPN_IPV4, tableType4.get());
        final Optional<BgpTableType> tableType6 = registry.getTableType(IPV6MCASTVPN.class);
        Assert.assertEquals(MVPN_IPV6, tableType6.get());

        tableTypeActivator.stopBGPTableTypeRegistryProvider();
        tableTypeActivator.close();
    }
}