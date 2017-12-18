/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.inet.codec.nexthop;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public final class Ipv6NextHopParserSerializer implements NextHopParserSerializer {
    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        return NextHopUtil.parseNextHop(buffer);

    }

    @Override
    public void serializeNextHop(final CNextHop nextHop, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(nextHop instanceof Ipv6NextHopCase,
                "cNextHop is not a Ipv6 NextHop object.");
        NextHopUtil.serializeNextHop(nextHop, byteAggregator);
    }
}
