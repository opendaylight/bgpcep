/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class Ipv6NextHopAttributeSerializer implements AttributeSerializer {
	public static final int TYPE = 3;
    public static final int ATTR_FLAGS = 64;

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        if (pathAttributes.getCNextHop() == null) return;
        if (pathAttributes.getCNextHop() instanceof Ipv6NextHopCase){
            byteAggregator.writeByte(UnsignedBytes.checkedCast(ATTR_FLAGS));
            byteAggregator.writeByte(UnsignedBytes.checkedCast(TYPE));
            ByteBuf nextHopBuffer = Unpooled.buffer();
            Ipv6NextHopCase nextHop = (Ipv6NextHopCase) pathAttributes.getCNextHop();
            if (nextHop.getIpv6NextHop().getGlobal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getGlobal()));
            }
            if (nextHop.getIpv6NextHop().getLinkLocal() != null) {
                nextHopBuffer.writeBytes(Ipv6Util.bytesForAddress(nextHop.getIpv6NextHop().getLinkLocal()));
            }
            byteAggregator.writeByte(UnsignedBytes.checkedCast(nextHopBuffer.writerIndex()));
            byteAggregator.writeBytes(nextHopBuffer);
        }
    }
}