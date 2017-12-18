/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.EmptyNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

/**
 * Utility class for of CNextHop serialization and parsing.
 */
public final class NextHopUtil {

    private NextHopUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes serialized cnextHop IP address as byte value into ByteBuf.
     *
     * @param cnextHop       next hop to be serialized
     * @param byteAggregator where the next hop will be written
     */
    public static void serializeNextHop(final CNextHop cnextHop, final ByteBuf byteAggregator) {
        if (cnextHop instanceof Ipv4NextHopCase) {
            byteAggregator.writeBytes(Ipv4Util.bytesForAddress(((Ipv4NextHopCase) cnextHop)
                    .getIpv4NextHop().getGlobal()));
        } else if (cnextHop instanceof Ipv6NextHopCase) {
            final Ipv6NextHop nextHop = ((Ipv6NextHopCase) cnextHop).getIpv6NextHop();
            Preconditions.checkArgument(nextHop.getGlobal() != null,
                    "Ipv6 Next Hop is missing Global address.");
            byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getGlobal()));
            if (nextHop.getLinkLocal() != null) {
                byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getLinkLocal()));
            }
        } else if (!(cnextHop instanceof EmptyNextHopCase)) {
            throw new IllegalArgumentException("Cannot serialize NEXT_HOP. Class not supported: " + cnextHop);
        }
    }

    /**
     * Parses CNextHop IP address from given ByteBuf.
     *
     * @param buffer contains byte array representation of CNextHop
     * @return CNexthop object
     */
    public static CNextHop parseNextHop(final ByteBuf buffer) {
        switch (buffer.writerIndex()) {
            case Ipv4Util.IP4_LENGTH:
                return new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                        .setGlobal(Ipv4Util.addressForByteBuf(buffer)).build()).build();
            case Ipv6Util.IPV6_LENGTH:
                return new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                        .setGlobal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
            case Ipv6Util.IPV6_LENGTH * 2:
                return new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                        new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForByteBuf(buffer))
                                .setLinkLocal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
            default:
                throw new IllegalArgumentException("Cannot parse NEXT_HOP attribute. Wrong bytes length: "
                        + buffer.writerIndex());
        }
    }
}
