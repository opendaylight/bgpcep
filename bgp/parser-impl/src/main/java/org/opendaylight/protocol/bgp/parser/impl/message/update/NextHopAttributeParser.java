/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.AttributeFlags;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class NextHopAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 3;

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
        Preconditions.checkArgument(buffer.readableBytes() == Ipv4Util.IP4_LENGTH, "Length of byte array for NEXT_HOP should be %s, but is %s", buffer.readableBytes(), Ipv4Util.IP4_LENGTH);
        builder.setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(
                new Ipv4NextHopBuilder().setGlobal(Ipv4Util.addressForBytes(ByteArray.readAllBytes(buffer))).build()).build());
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        CNextHop cNextHop = pathAttributes.getCNextHop();
        if (cNextHop == null) {
            return;
        }
        ByteBuf nextHopBuffer = Unpooled.buffer();
        if (cNextHop instanceof Ipv4NextHopCase) {
            byteAggregator.writeByte(AttributeFlags.TRANSITIVE);
            byteAggregator.writeByte(TYPE);
            Ipv4NextHopCase nextHop = (Ipv4NextHopCase) cNextHop;
            nextHopBuffer.writeBytes(Ipv4Util.bytesForAddress(nextHop.getIpv4NextHop().getGlobal()));
            byteAggregator.writeByte(nextHopBuffer.writerIndex());
            byteAggregator.writeBytes(nextHopBuffer);
        } else if (cNextHop instanceof Ipv6NextHopCase) {
            byteAggregator.writeByte(AttributeFlags.TRANSITIVE);
            byteAggregator.writeByte(TYPE);
            Ipv6NextHopCase nextHop = (Ipv6NextHopCase) cNextHop;
            if (nextHop.getIpv6NextHop().getGlobal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getGlobal()));
            }
            if (nextHop.getIpv6NextHop().getLinkLocal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getLinkLocal()));
            }
            byteAggregator.writeByte(nextHopBuffer.writerIndex());
            byteAggregator.writeBytes(nextHopBuffer);
        }

    }
}
