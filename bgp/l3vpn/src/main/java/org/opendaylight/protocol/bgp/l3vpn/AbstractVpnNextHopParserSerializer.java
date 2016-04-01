/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;

/**
 * @author Kevin Wang
 */
public abstract class AbstractVpnNextHopParserSerializer implements NextHopParserSerializer {
    private final int IP_ADDR_LENGTH;
    private final Class<?> IP_NEXT_HOP_CASE_CLAZZ;

    protected AbstractVpnNextHopParserSerializer(int ipAddrLength, Class<?> ipNextHopCaseClazz) {
        IP_ADDR_LENGTH = ipAddrLength;
        IP_NEXT_HOP_CASE_CLAZZ = ipNextHopCaseClazz;
    }

    @Override
    public CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() == (IP_ADDR_LENGTH + RouteDistinguisherUtil.RD_LENGTH), "Length of byte array for NEXT_HOP should be %s, but is %s", IP_ADDR_LENGTH + RouteDistinguisherUtil.RD_LENGTH, buffer.readableBytes());
        buffer.readBytes(RouteDistinguisherUtil.RD_LENGTH);
        return NextHopUtil.parseNextHop(buffer.readBytes(IP_ADDR_LENGTH));
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(IP_NEXT_HOP_CASE_CLAZZ.isInstance(cNextHop), "cNextHop is not a VPN %s NextHop object.", IP_NEXT_HOP_CASE_CLAZZ.getSimpleName());
        byteAggregator.writeZero(RouteDistinguisherUtil.RD_LENGTH);
        NextHopUtil.serializeNextHop(cNextHop, byteAggregator);
    }
}
