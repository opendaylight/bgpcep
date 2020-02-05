/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;

/**
 * Handles Next Hop, by default use {@link NextHopUtil} which is handles Ipv4 and Ipv6 Next hop.
 */
@NonNullByDefault
public interface NextHopParserSerializer {
    /**
     * Parse Next hop from buffer.
     *
     * @param buffer Encoded Next Hop in ByteBuf.
     * @return CNextHop
     */
    default CNextHop parseNextHop(final ByteBuf buffer) throws BGPParsingException {
        return NextHopUtil.parseNextHop(buffer);
    }

    /**
     * Serialize Next Hop.
     *
     * @param cnextHop Next Hop container
     * @param byteAggregator return Encoded Next Hop in ByteBuf
     */
    default void serializeNextHop(final CNextHop cnextHop, final ByteBuf byteAggregator) {
        NextHopUtil.serializeNextHop(cnextHop, byteAggregator);
    }
}
