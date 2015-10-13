/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.next.hop;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.LinkstateIpv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.LinkstateIpv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.linkstate.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.linkstate.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;

public class LinkstateNextHopParser implements NextHopParserSerializer {
    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        if (buffer.readableBytes() == Ipv4Util.IP4_LENGTH) {
            return NextHopUtil.parseNextHop(buffer);
        }
        Preconditions.checkArgument(buffer.readableBytes() == Ipv6Util.IPV6_LENGTH * 2, "Length of byte array for" +
            " NEXT_HOP should be < %s, but is %s", buffer.readableBytes(), Ipv6Util.IPV6_LENGTH);
        return new LinkstateIpv6NextHopCaseBuilder().setIpv6NextHop(
            new Ipv6NextHopBuilder().setGlobal(Ipv6Util.addressForByteBuf(buffer)).setLinkLocal(Ipv6Util.addressForByteBuf(buffer)).build()).build();
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf byteAggregator) {
        if (cNextHop instanceof Ipv4NextHopCase) {
            NextHopUtil.serializeNextHop(cNextHop, byteAggregator);
        }
        Preconditions.checkArgument(cNextHop instanceof LinkstateIpv6NextHopCase, "cNextHop is not a Ipv6 NextHop object.");
        final Ipv6NextHop nextHop = ((LinkstateIpv6NextHopCase) cNextHop).getIpv6NextHop();
        Preconditions.checkArgument(nextHop.getGlobal() != null, "Ipv6 Next Hop is missing Global address.");
        byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getGlobal()));
        if (nextHop.getLinkLocal() != null) {
            byteAggregator.writeBytes(Ipv6Util.bytesForAddress(nextHop.getLinkLocal()));
        }
    }
}