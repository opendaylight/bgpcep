/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParser;
import org.opendaylight.protocol.bgp.parser.spi.NextHopSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.LinkstateNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.LinkstateNextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.c.next.hop.linkstate.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.CNextHop;

public class LinkstateNextHopParser implements NextHopParser, NextHopSerializer {
    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() == Ipv4Util.IP4_LENGTH, "Length of byte array for NEXT_HOP " +
            "should be %s, but is %s", buffer.readableBytes(), Ipv4Util.IP4_LENGTH);
        return new LinkstateNextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(Ipv4Util
            .addressForByteBuf(buffer)).build()).build();
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(cNextHop instanceof LinkstateNextHopCase, "cNextHop is not a Link " +
            "State NextHop Case object.");
        final Ipv4Address nextHop = ((LinkstateNextHopCase) cNextHop).getIpv4NextHop().getGlobal();
        Preconditions.checkArgument(nextHop != null, "Ipv4 Next Hop is missing Global address.");
        byteAggregator.writeBytes(Ipv4Util.bytesForAddress(nextHop));
    }
}
