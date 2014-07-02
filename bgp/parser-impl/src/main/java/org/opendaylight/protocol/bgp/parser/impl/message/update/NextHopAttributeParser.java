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

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
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
                new Ipv4NextHopBuilder().setGlobal(Ipv4Util.addressForByteBuf(buffer)).build()).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        final CNextHop cNextHop = ((PathAttributes) attribute).getCNextHop();
        if (cNextHop == null) {
            return;
        }
        final ByteBuf nextHopBuffer = Unpooled.buffer();
        if (cNextHop instanceof Ipv4NextHopCase) {
            final Ipv4NextHopCase nextHop = (Ipv4NextHopCase) cNextHop;
            nextHopBuffer.writeBytes(Ipv4Util.bytesForAddress(nextHop.getIpv4NextHop().getGlobal()));
        } else if (cNextHop instanceof Ipv6NextHopCase) {
            final Ipv6NextHopCase nextHop = (Ipv6NextHopCase) cNextHop;
            if (nextHop.getIpv6NextHop().getGlobal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getGlobal()));
            }
            if (nextHop.getIpv6NextHop().getLinkLocal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getLinkLocal()));
            }
        }
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, nextHopBuffer, byteAggregator);
    }
}
