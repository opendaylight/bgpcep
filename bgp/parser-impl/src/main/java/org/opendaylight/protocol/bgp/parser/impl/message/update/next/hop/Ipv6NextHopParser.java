/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.next.hop;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParser;
import org.opendaylight.protocol.bgp.parser.spi.NextHopSerializer;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class Ipv6NextHopParser implements NextHopParser, NextHopSerializer {
    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() > Ipv6Util.IPV6_LENGTH, "Length of byte array for NEXT_HOP" +
            " should be < %s, but is %s", buffer.readableBytes(), Ipv6Util.IPV6_LENGTH);
        if (buffer.readableBytes() > Ipv6Util.IPV6_LENGTH) {
            return new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(Ipv6Util
                .addressForByteBuf(buffer)).setLinkLocal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
        }
        return new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(cNextHop instanceof Ipv6NextHopCase, "cNextHop is not a Ipv6 NextHop object.");
        final Ipv6NextHop nextHop = ((Ipv6NextHopCase) cNextHop).getIpv6NextHop();
        Preconditions.checkArgument(nextHop.getGlobal() != null, "Ipv6 Next Hop is missing Global address.");
        byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getGlobal()));
        if (nextHop.getLinkLocal() != null) {
            byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getLinkLocal()));
        }
    }
}
