/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.EmptyNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
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
     * @param cnextHop next hop to be serialized
     * @param byteAggregator where the next hop will be written
     */
    public static void serializeNextHop(final CNextHop cnextHop, final ByteBuf byteAggregator) {
        if (cnextHop instanceof Ipv4NextHopCase) {
            final Ipv4NextHop nextHop = ((Ipv4NextHopCase) cnextHop).getIpv4NextHop();
            // it is possible next hop is null, e.g. empty container ( "ipv4-next-hop": {} )
            // TODO if null value is not allowed, it should be forbidden via commit cohort
            // and make the leaf mandatory
            if (nextHop != null && nextHop.getGlobal() != null) {
                byteAggregator.writeBytes(Ipv4Util.bytesForAddress(nextHop.getGlobal()));
            }
        } else if (cnextHop instanceof Ipv6NextHopCase) {
            final Ipv6NextHop nextHop = ((Ipv6NextHopCase) cnextHop).getIpv6NextHop();
            if (nextHop != null) {
                if (nextHop.getGlobal() != null) {
                    byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getGlobal()));
                    if (nextHop.getLinkLocal() != null) {
                        byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getLinkLocal()));
                    }
                } else if (nextHop.getLinkLocal() != null) {
                    // a link-local address shouldn't exist when global address is not there
                    throw new IllegalArgumentException("Ipv6 Next Hop is missing Global address.");
                }
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
            return new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(Ipv4Util.addressForByteBuf(buffer)).build()).build();
        case Ipv6Util.IPV6_LENGTH:
            return new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
        case Ipv6Util.IPV6_LENGTH * 2:
            return new Ipv6NextHopCaseBuilder().setIpv6NextHop(
                    new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForByteBuf(buffer)).setLinkLocal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
        default:
            throw new IllegalArgumentException("Cannot parse NEXT_HOP attribute. Wrong bytes length: " + buffer.writerIndex());
        }
    }
}
