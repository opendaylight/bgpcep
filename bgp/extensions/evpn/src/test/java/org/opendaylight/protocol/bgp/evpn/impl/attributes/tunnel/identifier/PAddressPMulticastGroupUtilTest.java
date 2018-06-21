/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IPV6;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.IP_ADDRESS;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PMSITunnelAttributeHandlerTestUtil.P_MULTICAST;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.PAddressPMulticastGroup;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class PAddressPMulticastGroupUtilTest {
    private static final byte[] IPV4_ADDRESS_EXPECTED = {
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };
    private static final byte[] IPV6_ADDRESS_EXPECTED = {
        0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    };

    private static final byte[] SENDER_P_MULTICAST_GROUP_EXPECTED = {
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x17, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };

    private static class MyPAddressPMulticastGroup implements PAddressPMulticastGroup {
        @Override
        public IpAddress getPAddress() {
            return IP_ADDRESS;
        }

        @Override
        public IpAddress getPMulticastGroup() {
            return P_MULTICAST;
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    }

    @Test
    public void parseIpAddress() throws Exception {
        final ByteBuf ipv4Actual = Unpooled.buffer();
        PAddressPMulticastGroupUtil.serializeIpAddress(IP_ADDRESS, ipv4Actual);
        assertArrayEquals(IPV4_ADDRESS_EXPECTED, ByteArray.readAllBytes(ipv4Actual));
        final ByteBuf ipv6Actual = Unpooled.buffer();
        PAddressPMulticastGroupUtil.serializeIpAddress(IPV6, ipv6Actual);
        assertArrayEquals(IPV6_ADDRESS_EXPECTED, ByteArray.readAllBytes(ipv6Actual));
        assertEquals(IP_ADDRESS, PAddressPMulticastGroupUtil.parseIpAddress(Ipv4Util.IP4_LENGTH,
                Unpooled.wrappedBuffer(IPV4_ADDRESS_EXPECTED)));
        assertEquals(IPV6, PAddressPMulticastGroupUtil.parseIpAddress(Ipv6Util.IPV6_LENGTH,
                Unpooled.wrappedBuffer(IPV6_ADDRESS_EXPECTED)));
        assertNull(PAddressPMulticastGroupUtil.parseIpAddress(6,
                Unpooled.wrappedBuffer(IPV4_ADDRESS_EXPECTED)));
    }

    @Test
    public void parseIpAddressPMulticastGroup() throws Exception {
        final PAddressPMulticastGroup pAddressPMulticastGroup = new MyPAddressPMulticastGroup();
        final ByteBuf pAddressPMulticastGroupActual = Unpooled.buffer();
        PAddressPMulticastGroupUtil.serializeSenderPMulticastGroup(pAddressPMulticastGroup,
                pAddressPMulticastGroupActual);
        assertArrayEquals(SENDER_P_MULTICAST_GROUP_EXPECTED, ByteArray.readAllBytes(pAddressPMulticastGroupActual));

        final PAddressPMulticastGroup actual = PAddressPMulticastGroupUtil
                .parseSenderPMulticastGroup(Unpooled.wrappedBuffer(SENDER_P_MULTICAST_GROUP_EXPECTED));
        assertEquals(IP_ADDRESS, actual.getPAddress());
        assertEquals(P_MULTICAST, actual.getPMulticastGroup());
    }
}