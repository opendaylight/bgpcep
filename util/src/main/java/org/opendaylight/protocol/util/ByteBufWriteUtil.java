/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;

/**
 * Utility class for ByteBuf's write methods.
 */
public final class ByteBufWriteUtil {

    public static final int SHORT_BYTES_LENGTH = Short.SIZE / Byte.SIZE;

    public static final int MEDIUM_BYTES_LENGTH = 3;

    public static final int INT_BYTES_LENGTH = Integer.SIZE / Byte.SIZE;

    public static final int LONG_BYTES_LENGTH = Long.SIZE / Byte.SIZE;

    public static final int FLOAT32_BYTES_LENGTH = INT_BYTES_LENGTH;

    public static final int ONE_BYTE_LENGTH = 1;

    public static final int IPV4_PREFIX_BYTE_LENGTH = Ipv4Util.IP4_LENGTH + 1;

    public static final int IPV6_PREFIX_BYTE_LENGTH = Ipv6Util.IPV6_LENGTH + 1;

    private ByteBufWriteUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes 32-bit integer <code>value</code> if not null, otherwise writes
     * zeros to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 4.
     *
     * @param value
     *            Integer value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeInt(final Integer value, final ByteBuf output) {
        if (value != null) {
            output.writeInt(value);
        } else {
            output.writeZero(INT_BYTES_LENGTH);
        }
    }

    /**
     * Writes 24-bit integer <code>value</code> if not null, otherwise writes
     * zeros to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 3.
     *
     * @param value
     *            Medium value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeMedium(final Integer value, final ByteBuf output) {
        if (value != null) {
            output.writeMedium(value);
        } else {
            output.writeZero(MEDIUM_BYTES_LENGTH);
        }
    }

    /**
     * Writes 16-bit short <code>value</code> if not null, otherwise writes
     * zeros to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 2.
     *
     * @param value
     *            Short value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeShort(final Short value, final ByteBuf output) {
        if (value != null) {
            output.writeShort(value);
        } else {
            output.writeZero(SHORT_BYTES_LENGTH);
        }
    }

    /**
     * Writes 64-bit long <code>value</code> if not null, otherwise writes zeros
     * to the <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by
     * 8.
     *
     * @param value
     *            Long value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeLong(final Long value, final ByteBuf output) {
        if (value != null) {
            output.writeLong(value);
        } else {
            output.writeZero(LONG_BYTES_LENGTH);
        }
    }

    /**
     * Writes boolean <code>value</code> if not null, otherwise writes zero to
     * the <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 1.
     *
     * @param value
     *            Boolean value to be written to the output.
     * @param output
     *            ByteBuf, where value or zero is written.
     */
    public static void writeBoolean(final Boolean value, final ByteBuf output) {
        if (value != null) {
            output.writeBoolean(value);
        } else {
            output.writeZero(ONE_BYTE_LENGTH);
        }
    }

    /**
     * Writes unsigned byte <code>value</code> if not null, otherwise writes
     * zero to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 1.
     *
     * @param value
     *            Short value to be write to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeUnsignedByte(final Short value, final ByteBuf output) {
        if (value != null) {
            output.writeByte(value);
        } else {
            output.writeZero(ONE_BYTE_LENGTH);
        }
    }

    /**
     * Writes unsigned 16-bit short integer <code>value</code> if not null,
     * otherwise writes zeros to the <code>output</code> ByteBuf. ByteBuf's
     * writerIndex is increased by 2.
     *
     * @param value
     *            Integer value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeUnsignedShort(final Integer value, final ByteBuf output) {
        if (value != null) {
            output.writeShort(value.shortValue());
        } else {
            output.writeZero(SHORT_BYTES_LENGTH);
        }
    }

    /**
     * Writes unsigned 32-bit integer <code>value</code> if not null, otherwise
     * writes zeros to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 4.
     *
     * @param value
     *            Long value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeUnsignedInt(final Long value, final ByteBuf output) {
        if (value != null) {
            output.writeInt(value.intValue());
        } else {
            output.writeZero(INT_BYTES_LENGTH);
        }
    }

    /**
     * Writes unsigned 64-bit integer <code>value</code> if not null, otherwise
     * writes zeros to the <code>output</code> ByteBuf. ByteBuf's writerIndex is
     * increased by 8.
     *
     * @param value
     *            BigInteger value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeUnsignedLong(final BigInteger value, final ByteBuf output) {
        if (value != null) {
            output.writeLong(value.longValue());
        } else {
            output.writeZero(LONG_BYTES_LENGTH);
        }
    }

    /**
     * Writes IPv4 address if not null, otherwise writes zeros to the
     * <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 4.
     *
     * @param ipv4Address
     *            IPv4 address to be written to the output.
     * @param output
     *            ByteBuf, where ipv4Address or zeros are written.
     */
    public static void writeIpv4Address(final Ipv4Address ipv4Address, final ByteBuf output) {
        if (ipv4Address != null) {
            output.writeBytes(Ipv4Util.bytesForAddress(ipv4Address));
        } else {
            output.writeZero(Ipv4Util.IP4_LENGTH);
        }
    }

    /**
     * Writes IPv4 address if not null, otherwise writes zeros to the
     * <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 4.
     *
     * @param ipv4Address
     *            IPv4 address to be written to the output.
     * @param output
     *            ByteBuf, where ipv4Address or zeros are written.
     */
    public static void writeIpv4Address(final Ipv4AddressNoZone ipv4Address, final ByteBuf output) {
        if (ipv4Address != null) {
            output.writeBytes(IetfInetUtil.INSTANCE.ipv4AddressNoZoneBytes(ipv4Address));
        } else {
            output.writeZero(Ipv4Util.IP4_LENGTH);
        }
    }

    /**
     * Writes IPv4 prefix if not null, otherwise writes zeros to the
     * <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 5.
     *
     * @param ipv4Prefix
     *            IPv4 prefix value to be written to the output. Prefix is
     *            written in the last byte.
     * @param output
     *            ByteBuf, where ipv4Prefix or zeros are written.
     */
    public static void writeIpv4Prefix(final Ipv4Prefix ipv4Prefix, final ByteBuf output) {
        if (ipv4Prefix != null) {
            output.writeBytes(Ipv4Util.bytesForPrefix(ipv4Prefix));
        } else {
            output.writeZero(IPV4_PREFIX_BYTE_LENGTH);
        }
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
    public static void writeIpv6Address(final Ipv6Address ipv6Address, final ByteBuf output) {
        if (ipv6Address != null) {
            output.writeBytes(Ipv6Util.bytesForAddress(ipv6Address));
        } else {
            output.writeZero(Ipv6Util.IPV6_LENGTH);
        }
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
            output.writeBytes(IetfInetUtil.INSTANCE.ipv6AddressNoZoneBytes(ipv6Address));
        } else {
            output.writeZero(Ipv6Util.IPV6_LENGTH);
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
            output.writeBytes(Ipv6Util.bytesForPrefix(ipv6Prefix));
        } else {
            output.writeZero(IPV6_PREFIX_BYTE_LENGTH);
        }
    }

    public static void writeMinimalPrefix(final Ipv4Prefix ipv4Prefix, final ByteBuf output) {
        final byte[] bytes = IetfInetUtil.INSTANCE.ipv4PrefixToBytes(ipv4Prefix);
        writeMinimalPrefix(output, bytes, bytes[Ipv4Util.IP4_LENGTH]);
    }

    public static void writeMinimalPrefix(final Ipv6Prefix ipv6Prefix, final ByteBuf output) {
        final byte[] bytes = IetfInetUtil.INSTANCE.ipv6PrefixToBytes(ipv6Prefix);
        writeMinimalPrefix(output, bytes, bytes[Ipv6Util.IPV6_LENGTH]);
    }

    private static void writeMinimalPrefix(final ByteBuf output, final byte[] bytes, final byte prefixBits) {
        output.writeByte(prefixBits);
        output.writeBytes(bytes, 0, Ipv4Util.prefixBitsToBytes(Byte.toUnsignedInt(prefixBits)));
    }

    /**
     * Writes Float32 <code>value</code> if not null, otherwise writes zeros to
     * the <code>output</code> ByteBuf. ByteBuf's writerIndex is increased by 4.
     *
     * @param value
     *            Float32 value to be written to the output.
     * @param output
     *            ByteBuf, where value or zeros are written.
     */
    public static void writeFloat32(final Float32 value, final ByteBuf output) {
        if (value != null) {
            output.writeBytes(value.getValue());
        } else {
            output.writeZero(FLOAT32_BYTES_LENGTH);
        }
    }
}
