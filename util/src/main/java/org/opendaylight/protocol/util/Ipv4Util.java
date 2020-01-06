/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

/**
 * Util class for creating generated Ipv4Address.
 */
public final class Ipv4Util {
    public static final int IP4_LENGTH = 4;
    public static final int IP4_BITS_LENGTH = 32;

    static final int PREFIX_BYTE_LENGTH = IP4_LENGTH + 1;

    private static final Ipv4Prefix EMPTY_PREFIX = new Ipv4Prefix("0.0.0.0/0");

    private Ipv4Util() {
        // Hidden on purpose
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv4Address.
     *
     * @param buffer containing Ipv4 address, starting at reader index
     * @return Ipv4Address
     */
    public static Ipv4Address addressForByteBuf(final ByteBuf buffer) {
        return IetfInetUtil.INSTANCE.ipv4AddressFor(ByteArray.readBytes(buffer, IP4_LENGTH));
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv4Address.
     *
     * @param buffer containing Ipv4 address, starting at reader index
     * @return Ipv4AddressNoZone
     */
    public static Ipv4AddressNoZone noZoneAddressForByteBuf(final ByteBuf buffer) {
        return IetfInetUtil.INSTANCE.ipv4AddressNoZoneFor(ByteArray.readBytes(buffer, IP4_LENGTH));
    }

    /**
     * From string ipAddress creates an InetAddress and puts it into ByteBuf.
     *
     * @param ipAddress Ipv4 address
     * @return ByteBuf with filled in bytes from ipAddress
     */
    public static ByteBuf byteBufForAddress(final Ipv4Address ipAddress) {
        return Unpooled.wrappedBuffer(bytesForAddress(ipAddress));
    }

    /**
     * Converts Ipv4Address to byte array.
     *
     * @param address Ipv4Address to be converted
     * @return byte array
     */
    public static byte[] bytesForAddress(final Ipv4Address address) {
        return IetfInetUtil.INSTANCE.ipv4AddressBytes(address);
    }

    public static int prefixBitsToBytes(final int bits) {
        if (bits % Byte.SIZE != 0) {
            return bits / Byte.SIZE + 1;
        }
        return bits / Byte.SIZE;
    }

    /**
     * Returns number of minimum bytes needed to cover all bits of prefix.
     */
    public static int getPrefixLengthBytes(final String prefix) {
        return prefixBitsToBytes(Ipv4Util.getPrefixLength(prefix));
    }

    /**
     * Converts Ipv4Prefix to byte array. Prefix length at the end.
     *
     * @param prefix Ipv4Prefix to be converted
     * @return byte array with prefix length at the end
     */
    public static byte[] bytesForPrefix(final Ipv4Prefix prefix) {
        return IetfInetUtil.INSTANCE.ipv4PrefixToBytes(prefix);
    }

    /**
     * Creates an Ipv4Prefix object from given byte array.
     *
     * @param bytes  IPv4 address
     * @param length prefix length
     * @return Ipv4Prefix object
     */
    public static Ipv4Prefix prefixForBytes(final byte[] bytes, final int length) {
        Preconditions.checkArgument(length <= bytes.length * Byte.SIZE);

        final byte[] tmp;
        if (bytes.length != IP4_LENGTH) {
            tmp = Arrays.copyOfRange(bytes, 0, IP4_LENGTH);
        } else {
            tmp = bytes;
        }

        return IetfInetUtil.INSTANCE.ipv4PrefixFor(tmp, length);
    }

    /**
     * Creates an Ipv4Prefix object from given ByteBuf. Prefix length is assumed to
     * be in the left most byte of the buffer.
     *
     * @param buf Buffer containing serialized prefix
     * @return Ipv4Prefix object
     */
    public static Ipv4Prefix prefixForByteBuf(final ByteBuf buf) {
        final int prefixLength = UnsignedBytes.toInt(buf.readByte());
        return prefixForByteBuf(buf, prefixLength);
    }

    /**
     * Creates an Ipv4Prefix object from given ByteBuf with specified NLRI length.
     *
     * @param buf Buffer containing serialized prefix
     * @param prefixLength Prefix length
     * @return Ipv4Prefix object
     */
    public static Ipv4Prefix prefixForByteBuf(final ByteBuf buf, final int prefixLength) {
        final int size = prefixLength / Byte.SIZE + (prefixLength % Byte.SIZE == 0 ? 0 : 1);
        final int readable = buf.readableBytes();
        Preconditions.checkArgument(size <= readable, "Illegal length of IP prefix: %s/%s", size, readable);

        final byte[] bytes = new byte[IP4_LENGTH];
        buf.readBytes(bytes, 0, size);
        return IetfInetUtil.INSTANCE.ipv4PrefixFor(bytes, prefixLength);
    }

    /**
     * Creates a list of Ipv4 Prefixes from given byte array.
     *
     * @param bytes to be converted to List of Ipv4Prefixes.
     * @return A list of Ipv4Prefixes
     */
    public static List<Ipv4Prefix> prefixListForBytes(final byte[] bytes) {
        if (bytes.length == 0) {
            return Collections.emptyList();
        }
        final List<Ipv4Prefix> list = new ArrayList<>();
        int byteOffset = 0;
        while (byteOffset < bytes.length) {
            final int bitLength = UnsignedBytes.toInt(bytes[byteOffset]);
            byteOffset += 1;
            // if length == 0, default route will be added
            if (bitLength == 0) {
                list.add(EMPTY_PREFIX);
                continue;
            }

            list.add(IetfInetUtil.INSTANCE.ipv4PrefixForShort(bytes, byteOffset, bitLength));
            byteOffset += bitLength / Byte.SIZE;
            if (bitLength % Byte.SIZE != 0) {
                byteOffset++;
            }

        }
        return list;
    }

    /**
     * Obtains prefix length from given string prefix.
     *
     * @param prefixValue value of prefix
     * @return prefix length
     */
    protected static int getPrefixLength(final String prefixValue) {
        final int sep = prefixValue.indexOf('/');
        return Integer.parseInt(prefixValue.substring(sep + 1, prefixValue.length()));
    }

    /**
     * Converts InetAddress to IpAddress.
     *
     * @param inetAddress address
     * @return IpAddress
     */
    public static IpAddress getIpAddress(final InetAddress inetAddress) {
        return IetfInetUtil.INSTANCE.ipAddressFor(inetAddress);
    }

    /**
     * Converts IpAddress and PortNumber to InetSocketAddress.
     *
     * @param ipAddress address
     * @param port      number
     * @return InetSocketAddress
     */
    public static InetSocketAddress toInetSocketAddress(final IpAddress ipAddress, final PortNumber port) {
        final String ipString = toStringIP(ipAddress);
        return new InetSocketAddress(InetAddresses.forString(ipString), port.getValue().toJava());
    }

    /**
     * Increment Address.
     *
     * @param ipv4Address String containing Ipv4Address
     * @return String containing Ipv4Address incremented by 1
     */
    public static String incrementIpv4Address(final String ipv4Address) {
        return InetAddresses.increment(InetAddresses.forString(ipv4Address)).getHostAddress();
    }

    /**
     * Increment Address.
     *
     * @param ipv4Address ipv4 address to be incremented
     * @return new ipv4 address
     */
    public static Ipv4Address incrementIpv4Address(final Ipv4Address ipv4Address) {
        return new Ipv4Address(incrementIpv4Address(ipv4Address.getValue()));
    }

    public static Ipv4Prefix incrementIpv4Prefix(final Ipv4Prefix ipv4Prefix) {
        final Map.Entry<Ipv4Address, Integer> splitIpv4Prefix = IetfInetUtil.INSTANCE.splitIpv4Prefix(ipv4Prefix);
        return IetfInetUtil.INSTANCE.ipv4PrefixFor(incrementIpv4Address(splitIpv4Prefix.getKey()),
                splitIpv4Prefix.getValue());
    }

    /**
     * Get string representation of IpAddress.
     *
     * @param ipAddress address
     * @return String value of Ipv4Address or Ipv6Address
     */
    public static String toStringIP(final IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }
        return ipAddress.getIpv6Address().getValue();
    }
}
