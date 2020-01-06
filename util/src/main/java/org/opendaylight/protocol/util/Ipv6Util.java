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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

/**
 * Util class for creating generated Ipv6Address.
 */
public final class Ipv6Util {
    public static final int IPV6_LENGTH = 16;
    public static final int IPV6_BITS_LENGTH = 128;

    static final int PREFIX_BYTE_LENGTH = IPV6_LENGTH + 1;

    private static final Ipv6Prefix EMPTY_PREFIX = new Ipv6Prefix("::/0");

    private Ipv6Util() {
        // Hidden on purpose
    }

    /**
     * Creates uncompressed IP Address.
     *
     * @param ip to be uncompressed
     * @return Ipv6Address with same, but uncompressed, value
     */
    public static Ipv6Address getFullForm(final Ipv6Address ip) {
        return new Ipv6Address(InetAddresses.forString(ip.getValue()).getHostAddress());
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv6Address.
     *
     * @param buffer containing Ipv6 address, starting at reader index
     * @return Ipv6Address
     */
    public static Ipv6Address addressForByteBuf(final ByteBuf buffer) {
        return IetfInetUtil.INSTANCE.ipv6AddressFor(ByteArray.readBytes(buffer, IPV6_LENGTH));
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv6AddressNoZone.
     *
     * @param buffer containing Ipv6 address, starting at reader index
     * @return Ipv6AddressNoZone
     */
    public static Ipv6AddressNoZone noZoneAddressForByteBuf(final ByteBuf buffer) {
        return IetfInetUtil.INSTANCE.ipv6AddressNoZoneFor(ByteArray.readBytes(buffer, IPV6_LENGTH));
    }

    /**
     * From string ipAddress creates an InetAddress and puts it into ByteBuf.
     * @param ipAddress Ipv6 address
     * @return ByteBuf with filled in bytes from ipAddress
     */
    public static ByteBuf byteBufForAddress(final Ipv6Address ipAddress) {
        return Unpooled.wrappedBuffer(bytesForAddress(ipAddress));
    }

    /**
     * Converts Ipv6Address to byte array.
     *
     * @param address Ipv6Address to be converted
     * @return byte array
     */
    public static byte[] bytesForAddress(final Ipv6Address address) {
        return IetfInetUtil.INSTANCE.ipv6AddressBytes(address);
    }

    /**
     * Converts Ipv6Prefix to byte array. Prefix length at the end.
     *
     * @param prefix Ipv6Prefix to be converted
     * @return byte array with prefix length at the end
     */
    public static byte[] bytesForPrefix(final Ipv6Prefix prefix) {
        return IetfInetUtil.INSTANCE.ipv6PrefixToBytes(prefix);
    }

    /**
     * Creates an Ipv6Prefix object from given byte array.
     *
     * @param bytes IPv6 address
     * @param length prefix length
     * @return Ipv6Prefix object
     */
    public static Ipv6Prefix prefixForBytes(final byte[] bytes, final int length) {
        Preconditions.checkArgument(length <= bytes.length * Byte.SIZE);

        final byte[] tmp;
        if (bytes.length != IPV6_LENGTH) {
            tmp = Arrays.copyOfRange(bytes, 0, IPV6_LENGTH);
        } else {
            tmp = bytes;
        }

        return IetfInetUtil.INSTANCE.ipv6PrefixFor(tmp, length);
    }

    /**
     * Creates an Ipv6Prefix object from given ByteBuf. Prefix length is assumed to
     * be in the left most byte of the buffer.
     *
     * @param buf IPv6 address
     * @return Ipv6Prefix object
     */
    public static Ipv6Prefix prefixForByteBuf(final ByteBuf buf) {
        final int prefixLength = UnsignedBytes.toInt(buf.readByte());
        final int size = prefixLength / Byte.SIZE + (prefixLength % Byte.SIZE == 0 ? 0 : 1);
        final int readable = buf.readableBytes();
        Preconditions.checkArgument(size <= readable, "Illegal length of IP prefix: %s/%s", size, readable);

        final byte[] bytes = new byte[IPV6_LENGTH];
        buf.readBytes(bytes, 0, size);
        return IetfInetUtil.INSTANCE.ipv6PrefixFor(bytes, prefixLength);
    }

    /**
     * Creates a list of Ipv6 Prefixes from given byte array.
     *
     * @param bytes to be converted to List of Ipv6Prefixes.
     * @return A List of Ipv6Prefixes
     */
    public static List<Ipv6Prefix> prefixListForBytes(final byte[] bytes) {
        if (bytes.length == 0) {
            return Collections.emptyList();
        }
        final List<Ipv6Prefix> list = new ArrayList<>();
        int byteOffset = 0;
        while (byteOffset < bytes.length) {
            final int bitLength = UnsignedBytes.toInt(bytes[byteOffset]);
            byteOffset += 1;
            // if length == 0, default route will be added
            if (bitLength == 0) {
                list.add(EMPTY_PREFIX);
                continue;
            }
            list.add(IetfInetUtil.INSTANCE.ipv6PrefixForShort(bytes, byteOffset, bitLength));
            byteOffset += bitLength / Byte.SIZE;
            if (bitLength % Byte.SIZE != 0) {
                byteOffset++;
            }
        }
        return list;
    }
}
