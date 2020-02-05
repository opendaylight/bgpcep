/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet.codec.nexthop;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;

public final class Ipv4NextHopParserSerializer implements NextHopParserSerializer {
    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        final int readable = buffer.readableBytes();
        checkArgument(readable == Ipv4Util.IP4_LENGTH, "Length of byte array for NEXT_HOP should be %s, but is %s",
                readable, Ipv4Util.IP4_LENGTH);
        return NextHopUtil.parseNextHopIpv4(buffer);
    }

    @Override
    public void serializeNextHop(final CNextHop nextHop, final ByteBuf byteAggregator) {
        checkArgument(nextHop instanceof Ipv4NextHopCase, "cNextHop is not a Ipv4 NextHop object.");
        NextHopUtil.serializeNextHop(nextHop, byteAggregator);
    }
}
