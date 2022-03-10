/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn.mcast.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;

@RunWith(Parameterized.class)
public class L3vpnMcastNlriSerializerTest {
    private static final byte[] IPV4_EXPECTED = new byte[]{
        32, //length
        0, 1, 1, 2, 3, 4, 1, 2, //RD
        24, (byte) 0xac, 17, 1, 0,// Prefix
        0, 0, 0//trailing bits
    };
    private static final byte[] IPV6_EXPECTED = new byte[]{
        (byte) 0x80, //length
        0, 1, 1, 2, 3, 4, 1, 2, //RD
        64, 32, 1, 13, (byte) 0xb8, 0, 1, 0, 2,// Prefix
        0, 0, 0, 0, 0, 0, 0 //trailing bits
    };
    private static final RouteDistinguisher RD = new RouteDistinguisher(new RdIpv4("1.2.3.4:258"));
    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("2001:db8:1:2::/64"));
    private static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("172.17.1.0/24"));
    private static final L3vpnMcastDestination MCAST_IPV4_L3VPN_DESTINATION = new L3vpnMcastDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setPrefix(IPV4_PREFIX)
            .build();
    private static final L3vpnMcastDestination MCAST_IPV6_L3VPN_DESTINATION = new L3vpnMcastDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setPrefix(IPV6_PREFIX)
            .build();
    private final byte[] expectedArray;
    private final List<L3vpnMcastDestination> destination;

    public L3vpnMcastNlriSerializerTest(final byte[] expectedArray, final List<L3vpnMcastDestination> destination) {
        this.expectedArray = expectedArray;
        this.destination = destination;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return List.of(new Object[][] {
                { IPV4_EXPECTED, List.of(MCAST_IPV4_L3VPN_DESTINATION) },
                { IPV6_EXPECTED, List.of(MCAST_IPV6_L3VPN_DESTINATION) },
        });
    }

    @Test
    public void testL3vpnMcastNlriSerializer() {
        ByteBuf actual = Unpooled.buffer();
        L3vpnMcastNlriSerializer.serializeNlri(destination, actual);
        assertArrayEquals(expectedArray, ByteArray.getAllBytes(actual));
        assertEquals(destination,
                L3vpnMcastNlriSerializer.extractDest(Unpooled.copiedBuffer(expectedArray), false));
    }
}