/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;

public final class VpnIpv4NextHopParserSerializer implements NextHopParserSerializer {

    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() == (Ipv4Util.IP4_LENGTH + VpnIpv4NlriParser.RD_LENGTH), "Length of byte array for NEXT_HOP should be %s, but is %s", Ipv4Util.IP4_LENGTH + VpnIpv4NlriParser.RD_LENGTH, buffer.readableBytes());
        buffer.readBytes(VpnIpv4NlriParser.RD_LENGTH);
        return NextHopUtil.parseNextHop(buffer);
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(cNextHop instanceof Ipv4NextHopCase, "cNextHop is not a Ipv4 NextHop object.");
        RouteDistinguisherUtil.serializeRouteDistinquisher(new RouteDistinguisher("0.0.0.0:0"), byteAggregator);
        NextHopUtil.serializeNextHop(cNextHop, byteAggregator);
    }

}
