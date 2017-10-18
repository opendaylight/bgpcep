/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;

public final class FSIpv6SourcePrefixHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int SOURCE_PREFIX_VALUE = 2;

    @Override
    public void serializeType(final FlowspecType value, final ByteBuf output) {
        Preconditions.checkArgument(value instanceof SourceIpv6PrefixCase, "SourceIpv6PrefixCase class is mandatory!");
        output.writeByte(SOURCE_PREFIX_VALUE);
        writePrefix(((SourceIpv6PrefixCase) value).getSourcePrefix(), output);
    }

    static void writePrefix(final Ipv6Prefix prefix, final ByteBuf output) {
        final byte[] bytes = Ipv6Util.bytesForPrefix(prefix);
        final byte prefixBits = bytes[Ipv6Util.IPV6_LENGTH];
        output.writeByte(prefixBits);
        output.writeByte(0);
        output.writeBytes(bytes, 0, Ipv4Util.prefixBitsToBytes(Byte.toUnsignedInt(prefixBits)));
    }

    @Override
    public FlowspecType parseType(final ByteBuf buffer) {
        requireNonNull(buffer, "input buffer is null, missing data to parse.");
        return new SourceIpv6PrefixCaseBuilder().setSourcePrefix(parseIpv6Prefix(buffer)).build();
    }

    static Ipv6Prefix parseIpv6Prefix(final ByteBuf nlri) {
        final int bitLength = nlri.readUnsignedByte();
        nlri.readUnsignedByte();
        // FIXME: this does not look right if bitLenght % Byte.SIZE != 0
        return Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, bitLength / Byte.SIZE), bitLength);
    }
}
