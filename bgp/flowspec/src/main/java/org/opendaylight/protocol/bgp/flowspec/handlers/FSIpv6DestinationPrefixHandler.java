/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;

public final class FSIpv6DestinationPrefixHandler implements FlowspecTypeParser, FlowspecTypeSerializer {
    public static final int IPV6_DESTINATION_PREFIX_VALUE = 1;

    @Override
    public void serializeType(FlowspecType value, ByteBuf output) {
        Preconditions.checkArgument(value instanceof DestinationIpv6PrefixCase, "DestinationIpv6PrefixCase class is mandatory!");
        output.writeByte(IPV6_DESTINATION_PREFIX_VALUE);
        output.writeBytes(insertOffsetByte(Ipv6Util.bytesForPrefixBegin(((DestinationIpv6PrefixCase) value).getDestinationPrefix())));
    }

    @Override
    public FlowspecType parseType(ByteBuf buffer) {
        Preconditions.checkArgument(((int) buffer.readUnsignedByte()) == IPV6_DESTINATION_PREFIX_VALUE, "Destination prefix type does not match!");
        return new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(parseIpv6Prefix(buffer)).build();
    }

    private static Ipv6Prefix parseIpv6Prefix(final ByteBuf nlri) {
        final int bitLength = nlri.readByte();
        final int offset = nlri.readByte();
        nlri.readBytes(offset);
        return Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, bitLength / Byte.SIZE), bitLength);
    }

    private static byte[] insertOffsetByte(final byte[] ipPrefix) {
        // income <len, prefix>
        return Bytes.concat(new byte[] { ipPrefix[0] }, new byte[] { 0 }, ByteArray.subByte(ipPrefix, 1 , ipPrefix.length-1));
    }
}
