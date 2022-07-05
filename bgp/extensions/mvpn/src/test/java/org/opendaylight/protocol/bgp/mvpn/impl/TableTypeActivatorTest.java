/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;

public final class TableTypeActivatorTest {
    private static final BgpTableType MVPN_IPV4 = new BgpTableTypeImpl(
            Ipv4AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE);
    private static final BgpTableType MVPN_IPV6 = new BgpTableTypeImpl(
            Ipv6AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(IPV4MCASTVPN.VALUE, registry.getAfiSafiType(MVPN_IPV4));
        assertEquals(IPV6MCASTVPN.VALUE, registry.getAfiSafiType(MVPN_IPV6));

        assertEquals(MVPN_IPV4, registry.getTableType(IPV4MCASTVPN.VALUE));
        assertEquals(MVPN_IPV6, registry.getTableType(IPV6MCASTVPN.VALUE));
    }
}
