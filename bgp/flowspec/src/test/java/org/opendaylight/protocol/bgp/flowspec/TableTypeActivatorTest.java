/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.flowspec;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.SimpleBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.IPV4FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.IPV4L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.IPV6L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public class TableTypeActivatorTest {

    private static final BgpTableType IPV4_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class);
    private static final BgpTableType IPV6_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class);
    private static final BgpTableType IPV4_VPN_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class);
    private static final BgpTableType IPV6_VPN_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        final TableTypeActivator tableTypeActivator = new TableTypeActivator();
        final SimpleBGPTableTypeRegistryProvider registry = new SimpleBGPTableTypeRegistryProvider();
        tableTypeActivator.startBGPTableTypeRegistryProvider(registry);

        final Optional<Class<? extends AfiSafiType>> afiSafiType = registry.getAfiSafiType(IPV4_FLOW);
        Assert.assertEquals(IPV4FLOW.class, afiSafiType.get());
        final Optional<Class<? extends AfiSafiType>> afiSafiType2 = registry.getAfiSafiType(IPV6_FLOW);
        Assert.assertEquals(IPV6FLOW.class, afiSafiType2.get());
        final Optional<Class<? extends AfiSafiType>> afiSafiType3 = registry.getAfiSafiType(IPV4_VPN_FLOW);
        Assert.assertEquals(IPV4L3VPNFLOW.class, afiSafiType3.get());
        final Optional<Class<? extends AfiSafiType>> afiSafiType4 = registry.getAfiSafiType(IPV6_VPN_FLOW);
        Assert.assertEquals(IPV6L3VPNFLOW.class, afiSafiType4.get());

        final Optional<BgpTableType> tableType = registry.getTableType(IPV4FLOW.class);
        Assert.assertEquals(IPV4_FLOW, tableType.get());
        final Optional<BgpTableType> tableType2 = registry.getTableType(IPV6FLOW.class);
        Assert.assertEquals(IPV6_FLOW, tableType2.get());
        final Optional<BgpTableType> tableType3 = registry.getTableType(IPV4L3VPNFLOW.class);
        Assert.assertEquals(IPV4_VPN_FLOW, tableType3.get());
        final Optional<BgpTableType> tableType4 = registry.getTableType(IPV6L3VPNFLOW.class);
        Assert.assertEquals(IPV6_VPN_FLOW, tableType4.get());

        tableTypeActivator.stopBGPTableTypeRegistryProvider();
        tableTypeActivator.close();
    }

}
