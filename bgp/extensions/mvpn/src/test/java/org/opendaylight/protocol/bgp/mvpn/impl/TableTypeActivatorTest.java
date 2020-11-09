/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.DefaultBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
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
        try (var registry = new DefaultBGPTableTypeRegistryProvider(new TableTypeActivator())) {
            assertEquals(Optional.of(IPV4MCASTVPN.class), registry.getAfiSafiType(MVPN_IPV4));
            assertEquals(Optional.of(IPV6MCASTVPN.class), registry.getAfiSafiType(MVPN_IPV6));

            assertEquals(Optional.of(MVPN_IPV4), registry.getTableType(IPV4MCASTVPN.class));
            assertEquals(Optional.of(MVPN_IPV6), registry.getTableType(IPV6MCASTVPN.class));
        }
    }
}