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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

/**
 * Util class for creating generated Ipv4Address.
 */
public final class Ipv4Util {
    public static final int IP4_LENGTH = 4;
    private static final Ipv4Prefix EMPTY_PREFIX = new Ipv4Prefix("0.0.0.0/0");

    private Ipv4Util() {
        throw new UnsupportedOperationException();
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
     * From string ipAddress creates an InetAddress and puts it into ByteBuf.
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
            return (bits / Byte.SIZE) + 1;
        }
        return bits / Byte.SIZE;
    }

    /**
     * Returns number of minimum bytes needed to cover all bits of prefix.
     *
     * @param prefix
     * @return
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
     * Converts Ipv4Prefix to byte array. Prefix length at the beginning.
     * Prefix bytes are trimmed from the end to match prefix length.
     *
     * @param prefix Ipv4Prefix to be converted
     * @return byte array with the prefix length at the beginning
     *
     * @deprecated This is inefficient, refactor code to use {@link #bytesForAddress(Ipv4Address)} or
     *             {@link ByteBufWriteUtil#writeMinimalPrefix(Ipv4Prefix, ByteBuf)}.
     */
    @Deprecated
    public static byte[] bytesForPrefixBegin(final Ipv4Prefix prefix) {
        final byte[] addrWithPrefix = bytesForPrefix(prefix);
        return prefixedBytes(addrWithPrefix[IP4_LENGTH], addrWithPrefix);
    }

    static byte[] prefixedBytes(final byte prefixBits, final byte[] address) {
        if (prefixBits != 0) {
            final int prefixBytes = prefixBitsToBytes(Byte.toUnsignedInt(prefixBits));
            final byte[] ret = new byte[prefixBytes + 1];
            ret[0] = prefixBits;
            System.arraycopy(address, 0, ret, 1, prefixBytes);
            return ret;
        } else {
            return new byte[] { 0 };
        }
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
        final byte[] tmp = Arrays.copyOfRange(bytes, 0, IP4_LENGTH);
        return IetfInetUtil.INSTANCE.ipv4PrefixFor(tmp, length);
    }

    /**
     * Creates an Ipv4Prefix object from given ByteBuf. Prefix length is assumed to
     * be in the left most byte of the buffer.
     *
     * @param bytes IPv4 address
     * @return Ipv4Prefix object
     */
    public static Ipv4Prefix prefixForByteBuf(final ByteBuf bytes) {
        final int prefixLength = bytes.readByte();
        final int size = prefixLength / Byte.SIZE + ((prefixLength % Byte.SIZE == 0) ? 0 : 1);
        Preconditions.checkArgument(size <= bytes.readableBytes(), "Illegal length of IP prefix: %s", bytes.readableBytes());
        return prefixForBytes(ByteArray.readBytes(bytes, size), prefixLength);
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
            final int byteCount = (bitLength % Byte.SIZE != 0) ? (bitLength / Byte.SIZE) + 1 : bitLength / Byte.SIZE;
            list.add(prefixForBytes(ByteArray.subByte(bytes, byteOffset, byteCount), bitLength));
            byteOffset += byteCount;

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
     * @param inetAddress
     * @return IpAddress
     */
    public static IpAddress getIpAddress(final InetAddress inetAddress) {
        return IetfInetUtil.INSTANCE.ipAddressFor(inetAddress);
    }

    /**
     * Converts IpAddress and PortNumber to InetSocketAddress
     *
     * @param ipAddress
     * @param port
     * @return InetSocketAddress
     */
    public static InetSocketAddress toInetSocketAddress(final IpAddress ipAddress, final PortNumber port) {
        final String ipString;
        if (ipAddress.getIpv4Address() != null) {
            ipString = ipAddress.getIpv4Address().getValue();
        } else {
            ipString = ipAddress.getIpv6Address().getValue();
        }
        return new InetSocketAddress(InetAddresses.forString(ipString), port.getValue());
    }
}
