/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

/**
 * @author Kevin Wang
 */
public final class VpnIpv6NextHopParserSerializer implements NextHopParserSerializer {
    @Override
    public CNextHop parseNextHop(ByteBuf buffer) throws BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() == (Ipv6Util.IPV6_LENGTH + RouteDistinguisherUtil.RD_LENGTH), "Length of byte array for NEXT_HOP should be %s, but is %s", Ipv6Util.IPV6_LENGTH + RouteDistinguisherUtil.RD_LENGTH, buffer.readableBytes());
        buffer.readBytes(RouteDistinguisherUtil.RD_LENGTH);
        return NextHopUtil.parseNextHop(buffer.readBytes(Ipv6Util.IPV6_LENGTH));
    }

    @Override
    public void serializeNextHop(CNextHop cNextHop, ByteBuf byteAggregator) {
        Preconditions.checkArgument(cNextHop instanceof Ipv6NextHopCase, "cNextHop is not a VPN Ipv6 NextHop object.");
        byteAggregator.writeZero(RouteDistinguisherUtil.RD_LENGTH);
        NextHopUtil.serializeNextHop(cNextHop, byteAggregator);
    }
}
