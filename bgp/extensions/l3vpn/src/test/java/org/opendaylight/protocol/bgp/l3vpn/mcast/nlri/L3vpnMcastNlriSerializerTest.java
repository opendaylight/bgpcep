/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn.mcast.nlri;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;

public class L3vpnMcastNlriSerializerTest {
    private static final byte[] EXPECTED = new byte[]{
        (byte) 0x80,
        0, 1, 1, 2, 3, 4, 1, 2, //RD
        32, 1, 13, (byte) 0xb8, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 64, // Prefix
        0, 0, 0, 0, 0, 0, 0 //trailing bits
    };
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdIpv4("1.2.3.4:258"));
    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:2::/64"));
    private static final L3vpnMcastDestination MCAST_L3VPN_DESTINATION = new L3vpnMcastDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setPrefix(IPV6_PREFIX)
            .build();

    @Test
    public void testL3vpnMcastNlriSerializer() {
        ByteBuf actual = Unpooled.buffer();
        L3vpnMcastNlriSerializer.serializeNlri(Collections.singletonList(MCAST_L3VPN_DESTINATION), actual);
        assertArrayEquals(EXPECTED, ByteArray.getAllBytes(actual));
    }
}