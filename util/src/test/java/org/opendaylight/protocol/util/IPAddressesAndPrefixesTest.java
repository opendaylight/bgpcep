/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.common.Uint16;

public class IPAddressesAndPrefixesTest {

    @Test
    public void test3() {
        assertEquals("123.123.123.123", new Ipv4Address("123.123.123.123").getValue());
        assertEquals("2001::1", new Ipv6Address("2001::1").getValue());
    }

    @Test
    public void test4() throws UnknownHostException {
        assertNotNull(new IpPrefix(new Ipv4Prefix("123.123.123.123/32")).getIpv4Prefix());
        assertNotNull(new IpPrefix(new Ipv6Prefix("2001::1/120")).getIpv6Prefix());
    }

    @Test
    public void test5() {
        assertEquals("123.123.123.123/24", new Ipv4Prefix("123.123.123.123/24").getValue());
        assertEquals("2001::1/120", new Ipv6Prefix("2001::1/120").getValue());
    }

    @Test
    public void testPrefix4ForBytes() {
        byte[] bytes = new byte[]{123, 122, 4, 5};
        assertEquals(new Ipv4Prefix("123.122.4.5/8"), Ipv4Util.prefixForBytes(bytes, 8));
        assertArrayEquals(new byte[]{102, 0, 0, 0, 8}, Ipv4Util.bytesForPrefix(new Ipv4Prefix("102.0.0.0/8")));

        bytes = new byte[]{(byte) 255, (byte) 255, 0, 0};
        assertEquals(new Ipv4Prefix("255.255.0.0/16"), Ipv4Util.prefixForBytes(bytes, 16));

        assertArrayEquals(new byte[]{(byte) 255, (byte) 255, 0, 0, 16},
                Ipv4Util.bytesForPrefix(new Ipv4Prefix("255.255.0.0/16")));

        try {
            Ipv4Util.prefixForBytes(bytes, 200);
            fail();
        } catch (final IllegalArgumentException exc) {
            assertNull(exc.getMessage());
        }
    }

    @Test
    public void testBytesForPrefix4Begin() {
        byte[] bytes = new byte[]{123, 122, 4, 5};
        assertEquals(new Ipv4Prefix("123.122.4.5/8"), Ipv4Util.prefixForBytes(bytes, 8));

        bytes = new byte[]{(byte) 255, (byte) 255, 0, 0};
        assertEquals(new Ipv4Prefix("255.255.0.0/16"), Ipv4Util.prefixForBytes(bytes, 16));

        bytes = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertEquals(new Ipv4Prefix("0.0.0.0/0"), Ipv4Util.prefixForBytes(bytes, 0));
    }

    @Test
    public void testPrefixForByteBuf() {
        final ByteBuf bb = Unpooled.wrappedBuffer(new byte[]{
            0x0e, 123, 122,
            0x40, 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00,
            0x00,
            (byte) 0x80, 0x20, 0x01, 0x48, 0x60, 0x48, 0x60, 0x00, 0x01, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
            (byte) 0x88, (byte) 0x89
        });
        assertEquals(new Ipv4Prefix("123.122.0.0/14"), Ipv4Util.prefixForByteBuf(bb));
        assertEquals(new Ipv6Prefix("2001::/64"), Ipv6Util.prefixForByteBuf(bb));
        assertEquals(new Ipv4Prefix("0.0.0.0/0"), Ipv4Util.prefixForByteBuf(bb));
        assertEquals(new Ipv6Prefix("::/0"), Ipv6Util.prefixForByteBuf(bb));
        // test prefix length 128, as its signed byte value is -128
        assertEquals(new Ipv6Prefix("2001:4860:4860:1:20::8889/128"), Ipv6Util.prefixForByteBuf(bb));
    }

    @Test
    public void testAddressForByteBuf() {
        final ByteBuf bb = Unpooled.wrappedBuffer(new byte[]{123, 122, 4, 5, 0x20, (byte) 0x01, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01});
        assertEquals(new Ipv4Address("123.122.4.5"), Ipv4Util.addressForByteBuf(bb));
        assertEquals(new Ipv6Address("2001::1"), Ipv6Util.addressForByteBuf(bb));
    }

    @Test
    public void testByteBufForAddress() {
        final ByteBuf bb4 = Unpooled.wrappedBuffer(new byte[]{123, 122, 4, 5});
        final ByteBuf bb6 = Unpooled.wrappedBuffer(new byte[]{0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01});
        assertEquals(bb4, Ipv4Util.byteBufForAddress(new Ipv4AddressNoZone("123.122.4.5")));
        assertEquals(bb6, Ipv6Util.byteBufForAddress(new Ipv6AddressNoZone("2001::1")));
    }

    @Test
    public void testBytesForAddress() {
        assertArrayEquals(new byte[]{12, 58, (byte) 201, 99},
                Ipv4Util.bytesForAddress(new Ipv4AddressNoZone("12.58.201.99")));
        assertArrayEquals(new byte[]{0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01}, Ipv6Util.bytesForAddress(new Ipv6AddressNoZone("2001::1")));
    }

    @Test
    public void testPrefix6ForBytes() {
        final byte[] bytes = new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02};
        assertEquals(new Ipv6Prefix("2001:db8:1:2::/64"), Ipv6Util.prefixForBytes(bytes, 64));
        assertArrayEquals(new byte[]{0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40},
                Ipv6Util.bytesForPrefix(new Ipv6Prefix("2001:db8:1:2::/64")));
        assertArrayEquals(new byte[]{0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80},
                Ipv6Util.bytesForPrefix(new Ipv6Prefix("2001:db8:1:2::/128")));

        try {
            Ipv6Util.prefixForBytes(bytes, 200);
            fail();
        } catch (final IllegalArgumentException exc) {
            assertNull(exc.getMessage());
        }
    }

    @Test
    public void testBytesForPrefix6Begin() {
        byte[] bytes = new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02};
        assertEquals(new Ipv6Prefix("2001:db8:1:2::/64"), Ipv6Util.prefixForBytes(bytes, 64));

        bytes = new byte[]{(byte) 0};
        assertEquals(new Ipv6Prefix("::/0"), Ipv6Util.prefixForBytes(bytes, 0));
    }

    @Test
    public void testPrefixLength() {
        assertEquals(8, Ipv4Util.getPrefixLengthBytes("2001:db8:1:2::/64"));
        assertEquals(3, Ipv4Util.getPrefixLengthBytes("172.168.3.0/22"));
    }

    @Test
    public void testPrefixList4ForBytes() {
        final byte[] bytes = new byte[]{22, (byte) 172, (byte) 168, 3, 8, 12, 32, (byte) 192, (byte) 168, 35, 100};
        List<Ipv4Prefix> prefs = Ipv4Util.prefixListForBytes(bytes);
        assertEquals(
                Lists.newArrayList(new Ipv4Prefix("172.168.3.0/22"), new Ipv4Prefix("12.0.0.0/8"),
                        new Ipv4Prefix("192.168.35.100/32")), prefs);

        prefs = Ipv4Util.prefixListForBytes(new byte[]{});
        assertTrue(prefs.isEmpty());
    }

    @Test
    public void testPrefixList6ForBytes() {
        final byte[] bytes = new byte[]{0x40, 0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02, 0x40, 0x20,
            0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x01,};
        List<Ipv6Prefix> prefs = Ipv6Util.prefixListForBytes(bytes);
        assertEquals(prefs, Lists.newArrayList(new Ipv6Prefix("2001:db8:1:2::/64"),
            new Ipv6Prefix("2001:db8:1:1::/64")));

        prefs = Ipv6Util.prefixListForBytes(new byte[]{});
        assertTrue(prefs.isEmpty());
    }

    @Test
    public void testFullFormOfIpv6() {
        assertEquals(new Ipv6Address("0:0:0:0:0:0:0:1"), Ipv6Util.getFullForm(new Ipv6AddressNoZone("::1")));
    }

    @Test
    public void testInetAddressToIpAddress() {
        final IpAddressNoZone ipAddress = Ipv4Util.getIpAddress(InetAddresses.forString("123.42.13.8"));
        assertNotNull(ipAddress.getIpv4AddressNoZone());
        assertEquals(new Ipv4Address("123.42.13.8"), ipAddress.getIpv4AddressNoZone());

        final IpAddressNoZone ipAddress2 = Ipv4Util.getIpAddress(InetAddresses.forString("2001:db8:1:2::"));
        assertNotNull(ipAddress2.getIpv6AddressNoZone());
        assertEquals(new Ipv6Address("2001:db8:1:2::"), ipAddress2.getIpv6AddressNoZone());
    }

    @Test
    public void testToInetSocketAddress() {
        final InetSocketAddress isa = Ipv4Util.toInetSocketAddress(
            new IpAddressNoZone(new Ipv4AddressNoZone("123.42.13.8")), new PortNumber(Uint16.TEN));
        assertEquals(10, isa.getPort());
        assertEquals("123.42.13.8", InetAddresses.toAddrString(isa.getAddress()));

        final InetSocketAddress isa2 = Ipv4Util.toInetSocketAddress(
            new IpAddressNoZone(new Ipv6AddressNoZone("2001:db8:1:2::")), new PortNumber(Uint16.TEN));
        assertEquals(10, isa2.getPort());
        assertEquals("2001:db8:1:2::", InetAddresses.toAddrString(isa2.getAddress()));
    }
}
