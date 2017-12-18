/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.SimpleBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

public class TableTypeActivatorTest {

    private static final BgpTableType IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.class,
            MplsLabeledVpnSubsequentAddressFamily.class);
    private static final BgpTableType IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.class,
            MplsLabeledVpnSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        final TableTypeActivator tableTypeActivator = new TableTypeActivator();
        final SimpleBGPTableTypeRegistryProvider registry = new SimpleBGPTableTypeRegistryProvider();
        tableTypeActivator.startBGPTableTypeRegistryProvider(registry);

        final Optional<Class<? extends AfiSafiType>> afiSafiType = registry.getAfiSafiType(IPV4);
        Assert.assertEquals(L3VPNIPV4UNICAST.class, afiSafiType.get());
        final Optional<Class<? extends AfiSafiType>> afiSafiType2 = registry.getAfiSafiType(IPV6);
        Assert.assertEquals(L3VPNIPV6UNICAST.class, afiSafiType2.get());

        final Optional<BgpTableType> tableType = registry.getTableType(L3VPNIPV4UNICAST.class);
        Assert.assertEquals(IPV4, tableType.get());
        final Optional<BgpTableType> tableType2 = registry.getTableType(L3VPNIPV6UNICAST.class);
        Assert.assertEquals(IPV6, tableType2.get());

        tableTypeActivator.stopBGPTableTypeRegistryProvider();
        tableTypeActivator.close();
    }

}
