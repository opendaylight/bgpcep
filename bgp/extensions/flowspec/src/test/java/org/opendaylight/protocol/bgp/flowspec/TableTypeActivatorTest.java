/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.DefaultBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.class,
        FlowspecSubsequentAddressFamily.class);
    private static final BgpTableType IPV6_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.class,
        FlowspecSubsequentAddressFamily.class);
    private static final BgpTableType IPV4_VPN_FLOW = new BgpTableTypeImpl(Ipv4AddressFamily.class,
        FlowspecL3vpnSubsequentAddressFamily.class);
    private static final BgpTableType IPV6_VPN_FLOW = new BgpTableTypeImpl(Ipv6AddressFamily.class,
        FlowspecL3vpnSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        try (var registry = new DefaultBGPTableTypeRegistryProvider(new TableTypeActivator())) {
            assertEquals(Optional.of(IPV4FLOW.class), registry.getAfiSafiType(IPV4_FLOW));
            assertEquals(Optional.of(IPV6FLOW.class), registry.getAfiSafiType(IPV6_FLOW));
            assertEquals(Optional.of(IPV4L3VPNFLOW.class), registry.getAfiSafiType(IPV4_VPN_FLOW));
            assertEquals(Optional.of(IPV6L3VPNFLOW.class), registry.getAfiSafiType(IPV6_VPN_FLOW));

            assertEquals(Optional.of(IPV4_FLOW), registry.getTableType(IPV4FLOW.class));
            assertEquals(Optional.of(IPV6_FLOW), registry.getTableType(IPV6FLOW.class));
            assertEquals(Optional.of(IPV4_VPN_FLOW), registry.getTableType(IPV4L3VPNFLOW.class));
            assertEquals(Optional.of(IPV6_VPN_FLOW), registry.getTableType(IPV6L3VPNFLOW.class));
        }
    }

}
