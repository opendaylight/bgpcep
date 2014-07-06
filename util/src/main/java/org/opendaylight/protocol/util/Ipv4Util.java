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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;

/**
 * Util class for creating generated Ipv4Address.
 */
public final class Ipv4Util {

    private Ipv4Util() {
    }

    public static final int IP4_LENGTH = 4;

    /**
     * Converts byte array to Inet4Address.
     *
     * @param bytes to be converted
     * @return InetAddress instance
     * @throws IllegalArgumentException if {@link UnknownHostException} is thrown.
     */
    private static InetAddress getAddress(final byte[] bytes) {
        try {
            return Inet4Address.getByAddress(bytes);
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Failed to construct IPv4 address", e);
        }
    }

    /**
     * Converts byte array to Ipv4Address.
     *
     * @param bytes to be converted to Ipv4Address
     * @return Ipv4Address
     */
    public static Ipv4Address addressForBytes(final byte[] bytes) {
        return new Ipv4Address(InetAddresses.toAddrString(getAddress(bytes)));
    }

    /**
     * Reads from ByteBuf buffer and converts bytes to Ipv4Address.
     *
     * @param buffer containing Ipv4 address, starting at reader index
     * @return Ipv4Address
     */
    public static Ipv4Address addressForByteBuf(final ByteBuf buffer) {
        return addressForBytes(ByteArray.readBytes(buffer, IP4_LENGTH));
    }

    /**
     * Converts Ipv4Address to byte array.
     *
     * @param address Ipv4Address to be converted
     * @return byte array
     */
    public static byte[] bytesForAddress(final Ipv4Address address) {
        final InetAddress a = InetAddresses.forString(address.getValue());
        Preconditions.checkArgument(a instanceof Inet4Address);
        return a.getAddress();
    }

    /**
     * Returns number of minimum bytes needed to cover all bits of prefix.
     *
     * @param prefix
     * @return
     */
    public static int getPrefixLengthBytes(final String prefix) {
        int bits = Ipv4Util.getPrefixLength(prefix);
        if (bits % 8 != 0) {
            return (bits / 8) + 1;
        }
        return bits / 8;
    }

    /**
     * Converts Ipv4Prefix to byte array of length equal to prefix length value.
     *
     * @param ipv4Prefix Ipv4Prefix to be converted
     * @return byte array
     */
    public static byte[] bytesForPrefixByPrefixLength(Ipv4Prefix ipv4Prefix) {
        return ByteArray.subByte(bytesForPrefix(ipv4Prefix), 0,
                getPrefixLengthBytes(ipv4Prefix.getValue()));
    }

    /**
     * Converts Ipv4Prefix to byte array. Prefix length at the end.
     *
     * @param prefix Ipv4Prefix to be converted
     * @return byte array with prefix length at the end
     */
    public static byte[] bytesForPrefix(final Ipv4Prefix prefix) {
        final String p = prefix.getValue();
        final int sep = p.indexOf('/');
        final InetAddress a = InetAddresses.forString(p.substring(0, sep));
        Preconditions.checkArgument(a instanceof Inet4Address);
        final byte[] bytes = a.getAddress();
        return Bytes.concat(bytes, new byte[] { Byte.valueOf(p.substring(sep + 1, p.length())) });
    }

    /**
     * Converts Ipv4Prefix to byte array. Prefix length at the beginning.
     * Prefix bytes are trimmed from the end to match prefix length.
     *
     * @param prefix Ipv4Prefix to be converted
     * @return byte array with the prefix length at the beginning
     */
    public static byte[] bytesForPrefixBegin(final Ipv4Prefix prefix) {
        final String p = prefix.getValue();
        final int sep = p.indexOf('/');
        final InetAddress a = InetAddresses.forString(p.substring(0, sep));
        Preconditions.checkArgument(a instanceof Inet4Address);
        final byte[] bytes = a.getAddress();
        final int length = getPrefixLength(p);
        return Bytes.concat(new byte[] { UnsignedBytes.checkedCast(length) }, ByteArray.subByte(bytes, 0 , getPrefixLengthBytes(p)));
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
        final InetAddress a = getAddress(tmp);
        return new Ipv4Prefix(InetAddresses.toAddrString(a) + '/' + length);
    }

    /**
     * Creates a list of Ipv4 Prefixes from given byte array.
     *
     * @param bytes to be converted to List of Ipv4Prefixes.
     * @return List<Ipv4Prefix>
     */
    public static List<Ipv4Prefix> prefixListForBytes(final byte[] bytes) {
        if (bytes.length == 0) {
            return Collections.emptyList();
        }
        final List<Ipv4Prefix> list = Lists.newArrayList();
        int byteOffset = 0;
        while (byteOffset < bytes.length) {
            final int bitLength = UnsignedBytes.toInt(ByteArray.subByte(bytes, byteOffset, 1)[0]);
            byteOffset += 1;
            final int byteCount = (bitLength % Byte.SIZE != 0) ? (bitLength / Byte.SIZE) + 1 : bitLength / Byte.SIZE;
            list.add(prefixForBytes(ByteArray.subByte(bytes, byteOffset, byteCount), bitLength));
            byteOffset += byteCount;
        }
        return list;
    }

    /**
     * Obtains prefix length from given prefix.
     *
     * @param prefix
     * @return prefix length
     */
    public static int getPrefixLength(final IpPrefix prefix) {
        String p = "";
        if (prefix.getIpv4Prefix() != null) {
            p = prefix.getIpv4Prefix().getValue();
        } else {
            p = prefix.getIpv6Prefix().getValue();
        }
        return getPrefixLength(p);
    }

    /**
     * Obtains prefix length from given string prefix.
     *
     * @param prefixValue value of prefix
     * @return prefix length
     */
    public static int getPrefixLength(final String prefixValue) {
        final int sep = prefixValue.indexOf('/');
        return Integer.valueOf(prefixValue.substring(sep + 1, prefixValue.length()));
    }
}
