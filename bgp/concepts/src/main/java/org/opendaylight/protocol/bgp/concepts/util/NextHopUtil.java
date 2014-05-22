/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts.util;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Utility class for of CNextHop attribute serialization.
* */
public class NextHopUtil {

    private NextHopUtil(){}


    public static void serializeNextHop(DataObject attribute,ByteBuf byteAggregator){
        serializeNextHop((CNextHop)attribute,byteAggregator);
    }

    /**
     * Writes serialized cnextHop attribute ip addres as byte value into byteAggregator without serializing attributes
     * length.
     * @param cnextHop
     * @param byteAggregator
     */
    public static void serializeNextHopSimple(CNextHop cnextHop,ByteBuf byteAggregator){
        ByteBuf nextHopBuffer = Unpooled.buffer();
        if (cnextHop instanceof Ipv4NextHopCase) {
            Ipv4NextHopCase nextHop = (Ipv4NextHopCase) cnextHop;
            serializeIpv4NextHop(nextHop,nextHopBuffer);
        }

        if (cnextHop instanceof Ipv6NextHopCase){
            Ipv6NextHopCase nextHop = (Ipv6NextHopCase) cnextHop;
            serializeIpv6NextHop(nextHop,nextHopBuffer);
        }
        byteAggregator.writeBytes(nextHopBuffer);
    }
    /**
     * Writes serialized cnextHop attribute ip addres as byte value into byteAggregator with serializing attributes
     * length.
     * @param cnextHop
     * @param byteAggregator
     */
    public static void serializeNextHop(CNextHop cnextHop,ByteBuf byteAggregator){
        ByteBuf nextHopBuffer = Unpooled.buffer();
        serializeNextHopSimple(cnextHop,nextHopBuffer);
        byteAggregator.writeByte(UnsignedBytes.checkedCast(nextHopBuffer.writerIndex()));
        byteAggregator.writeBytes(nextHopBuffer);
    }

    /**
     * Writes nextHopCase attributes ipv4 address as bytes to byteAggregator.
     * @param nextHopCase
     * @param byteAggregator
     */
    public static void serializeIpv4NextHop(Ipv4NextHopCase nextHopCase,ByteBuf byteAggregator){
        byteAggregator.writeBytes(Ipv4Util.bytesForAddress(nextHopCase.getIpv4NextHop().getGlobal()));
    }
    /**
     * Writes nextHopCase attributes ipv6 address as bytes to byteAggregator.
     * @param nextHopCase
     * @param byteAggregator
     */
    public static void serializeIpv6NextHop(Ipv6NextHopCase nextHopCase,ByteBuf byteAggregator) {
        if (nextHopCase.getIpv6NextHop().getGlobal()!=null) {
            byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHopCase.getIpv6NextHop().getGlobal()));
        }
        if (nextHopCase.getIpv6NextHop().getLinkLocal()!=null) {
            byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHopCase.getIpv6NextHop().getLinkLocal()));
        }
    }

    public static void writeTLV(int t, ByteBuf value,ByteBuf byteAggregator){
        byteAggregator.writeShort(t);
        byteAggregator.writeShort(value.writerIndex());
        byteAggregator.writeBytes(value);
    }

}
