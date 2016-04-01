/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;

/**
 * @author Kevin Wang
 */
public final class VpnIpv6NextHopParserSerializer implements NextHopParserSerializer {
    @Override
    public CNextHop parseNextHop(ByteBuf buffer) throws BGPParsingException {
        return null;
    }

    @Override
    public void serializeNextHop(CNextHop cNextHop, ByteBuf byteAggregator) {

    }
}
