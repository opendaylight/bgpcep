/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.net.InetAddresses;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
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
    public static Ipv6AddressNoZone getFullForm(final Ipv6AddressNoZone ip) {
        return new Ipv6AddressNoZone(InetAddresses.forString(ip.getValue()).getHostAddress());
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv6Address.
     *
     * @param buffer containing Ipv6 address, starting at reader index
     * @return Ipv6Address
     */
    public static Ipv6AddressNoZone addressForByteBuf(final ByteBuf buffer) {
        return IetfInetUtil.ipv6AddressFor(ByteArray.readBytes(buffer, IPV6_LENGTH));
    }

    /**
     * From string ipAddress creates an InetAddress and puts it into ByteBuf.
     * @param ipAddress Ipv6 address
     * @return ByteBuf with filled in bytes from ipAddress
     */
    public static ByteBuf byteBufForAddress(final Ipv6AddressNoZone ipAddress) {
        return Unpooled.wrappedBuffer(bytesForAddress(ipAddress));
    }

    /**
     * Converts Ipv6Address to byte array.
     *
     * @param address Ipv6Address to be converted
     * @return byte array
     */
    public static byte[] bytesForAddress(final Ipv6AddressNoZone address) {
        return IetfInetUtil.ipv6AddressNoZoneBytes(address);
    }

    /**
     * Converts Ipv6Prefix to byte array. Prefix length at the end.
     *
     * @param prefix Ipv6Prefix to be converted
     * @return byte array with prefix length at the end
     */
    public static byte[] bytesForPrefix(final Ipv6Prefix prefix) {
        return IetfInetUtil.ipv6PrefixToBytes(prefix);
    }

    /**
     * Creates an Ipv6Prefix object from given byte array.
     *
     * @param bytes IPv6 address
     * @param length prefix length
     * @return Ipv6Prefix object
     */
    public static Ipv6Prefix prefixForBytes(final byte[] bytes, final int length) {
        checkArgument(length <= bytes.length * Byte.SIZE);
        return IetfInetUtil.ipv6PrefixFor(
            bytes.length == IPV6_LENGTH ? bytes : Arrays.copyOfRange(bytes, 0, IPV6_LENGTH), length);
    }

    /**
     * Creates an Ipv6Prefix object from given ByteBuf. Prefix length is assumed to
     * be in the left most byte of the buffer.
     *
     * @param buf IPv6 address
     * @return Ipv6Prefix object
     */
    public static Ipv6Prefix prefixForByteBuf(final ByteBuf buf) {
        final int prefixLength = buf.readUnsignedByte();
        final int size = Ipv4Util.prefixBitsToBytes(prefixLength);
        final int readable = buf.readableBytes();
        checkArgument(size <= readable, "Illegal length of IP prefix: %s/%s", size, readable);

        final byte[] bytes = new byte[IPV6_LENGTH];
        buf.readBytes(bytes, 0, size);
        return IetfInetUtil.ipv6PrefixFor(bytes, prefixLength);
    }

    /**
     * Creates a list of Ipv6 Prefixes from given byte array.
     *
     * @param bytes to be converted to List of Ipv6Prefixes.
     * @return A List of Ipv6Prefixes
     */
    public static List<Ipv6Prefix> prefixListForBytes(final byte[] bytes) {
        if (bytes.length == 0) {
            return List.of();
        }
        final var list = new ArrayList<Ipv6Prefix>();
        int byteOffset = 0;
        while (byteOffset < bytes.length) {
            final int bitLength = Byte.toUnsignedInt(bytes[byteOffset]);
            byteOffset += 1;
            // if length == 0, default route will be added
            if (bitLength == 0) {
                list.add(EMPTY_PREFIX);
                continue;
            }
            list.add(IetfInetUtil.ipv6PrefixForShort(bytes, byteOffset, bitLength));
            byteOffset += bitLength / Byte.SIZE;
            if (bitLength % Byte.SIZE != 0) {
                byteOffset++;
            }
        }
        return list;
    }

    /**
     * Writes IPv6 address if not null, otherwise writes zeros to the
     * <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 16.
     *
     * @param ipv6Address
     *            IPv6 address to be written to the output.
     * @param output
     *            ByteBuf, where ipv6Address or zeros are written.
     */
    public static void writeIpv6Address(final Ipv6AddressNoZone ipv6Address, final ByteBuf output) {
        if (ipv6Address != null) {
            output.writeBytes(IetfInetUtil.ipv6AddressNoZoneBytes(ipv6Address));
        } else {
            output.writeZero(IPV6_LENGTH);
        }
    }

    /**
     * Writes IPv6 prefix if not null, otherwise writes zeros to the
     * <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 17.
     *
     * @param ipv6Prefix
     *            IPv6 prefix to be written to the output. Prefix is written in
     *            the last byte.
     * @param output
     *            ByteBuf, where ipv6Prefix or zeros are written.
     */
    public static void writeIpv6Prefix(final Ipv6Prefix ipv6Prefix, final ByteBuf output) {
        if (ipv6Prefix != null) {
            output.writeBytes(bytesForPrefix(ipv6Prefix));
        } else {
            output.writeZero(PREFIX_BYTE_LENGTH);
        }
    }

    public static void writeMinimalPrefix(final Ipv6Prefix ipv6Prefix, final ByteBuf output) {
        final var bytes = IetfInetUtil.ipv6PrefixToBytes(ipv6Prefix);
        Ipv4Util.writeMinimalPrefix(output, bytes, bytes[IPV6_LENGTH]);
    }
}
