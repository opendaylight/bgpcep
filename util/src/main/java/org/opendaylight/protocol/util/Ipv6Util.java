/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

/**
 * Util class for creating generated Ipv6Address.
 */
public final class Ipv6Util {

    private Ipv6Util() {
        throw new UnsupportedOperationException();
    }

    public static final int IPV6_LENGTH = 16;

    /**
     * Converts byte array to Inet6Address.
     *
     * @param bytes to be converted
     * @return InetAddress instance
     * @throws IllegalArgumentException if {@link UnknownHostException} is thrown.
     */
    private static InetAddress getAddress(final byte[] bytes) {
        try {
            return Inet6Address.getByAddress(bytes);
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Failed to construct IPv6 address", e);
        }
    }

    /**
     * Creates uncompressed IP Address
     *
     * @param ip to be uncompressed
     * @return Ipv6Address with same, but uncompressed, value
     */
    public static Ipv6Address getFullForm(final Ipv6Address ip) {
        return new Ipv6Address(getAddress(Ipv6Util.bytesForAddress(ip)).toString().substring(1));
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv6Address.
     *
     * @param buffer containing Ipv6 address, starting at reader index
     * @return Ipv6Address
     */
    public static Ipv6Address addressForByteBuf(final ByteBuf buffer) {
        return new Ipv6Address(InetAddresses.toAddrString(getAddress((ByteArray.readBytes(buffer, IPV6_LENGTH)))));
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
        final InetAddress a = InetAddresses.forString(address.getValue());
        Preconditions.checkArgument(a instanceof Inet6Address);
        return a.getAddress();
    }

    /**
     * Converts Ipv6Prefix to byte array. Prefix length at the end.
     *
     * @param prefix Ipv6Prefix to be converted
     * @return byte array with prefix length at the end
     */
    public static byte[] bytesForPrefix(final Ipv6Prefix prefix) {
        final String p = prefix.getValue();
        final int sep = p.indexOf('/');
        final InetAddress a = InetAddresses.forString(p.substring(0, sep));
        Preconditions.checkArgument(a instanceof Inet6Address);
        final byte[] bytes = a.getAddress();
        return Bytes.concat(bytes, new byte[] { UnsignedBytes.parseUnsignedByte(p.substring(sep + 1, p.length())) });
    }

    /**
     * Converts Ipv6Prefix to byte array. Prefix length at the beginning.
     * Prefix bytes are trimmed from the end to match prefix length.
     *
     * @param prefix Ipv6Prefix to be converted
     * @return byte array with the prefix length at the beginning
     */
    public static byte[] bytesForPrefixBegin(final Ipv6Prefix prefix) {
        final String p = prefix.getValue();
        final int length = Ipv4Util.getPrefixLength(p);
        if (length == 0) {
            return new byte[] { 0 };
        }
        final int sep = p.indexOf('/');
        final InetAddress a = InetAddresses.forString(p.substring(0, sep));
        Preconditions.checkArgument(a instanceof Inet6Address);
        final byte[] bytes = a.getAddress();
        return Bytes.concat(new byte[] { UnsignedBytes.checkedCast(length) }, ByteArray.subByte(bytes, 0 , Ipv4Util.getPrefixLengthBytes(p)));
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
        final byte[] tmp = Arrays.copyOfRange(bytes, 0, IPV6_LENGTH);
        final InetAddress a = getAddress(tmp);
        return new Ipv6Prefix(InetAddresses.toAddrString(a) + '/' + length);
    }

    /**
     * Creates an Ipv6Prefix object from given ByteBuf. Prefix length is assumed to
     * be in the left most byte of the buffer.
     *
     * @param bytes IPv6 address
     * @return Ipv6Prefix object
     */
    public static Ipv6Prefix prefixForByteBuf(final ByteBuf bytes) {
        final int prefixLength = bytes.readByte();
        final int size = prefixLength / Byte.SIZE + ((prefixLength % Byte.SIZE == 0) ? 0 : 1);
        Preconditions.checkArgument(size <= bytes.readableBytes(), "Illegal length of IP prefix: %s", bytes.readableBytes());
        return Ipv6Util.prefixForBytes(ByteArray.readBytes(bytes, size), prefixLength);
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
        final List<Ipv6Prefix> list = Lists.newArrayList();
        int byteOffset = 0;
        while (byteOffset < bytes.length) {
            final int bitLength = UnsignedBytes.toInt(ByteArray.subByte(bytes, byteOffset, 1)[0]);
            byteOffset += 1;
            // if length == 0, default route will be added
            if (bitLength == 0) {
                list.add(new Ipv6Prefix("::/0"));
                continue;
            }
            final int byteCount = (bitLength % Byte.SIZE != 0) ? (bitLength / Byte.SIZE) + 1 : bitLength / Byte.SIZE;
            list.add(prefixForBytes(ByteArray.subByte(bytes, byteOffset, byteCount), bitLength));
            byteOffset += byteCount;

        }
        return list;
    }
}
