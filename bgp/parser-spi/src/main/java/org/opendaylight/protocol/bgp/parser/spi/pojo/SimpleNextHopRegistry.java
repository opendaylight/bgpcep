/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParser;
import org.opendaylight.protocol.bgp.parser.spi.NextHopRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NextHopSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleNextHopRegistry implements NextHopRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleNextHopRegistry.class);
    private final Table<Class<? extends AddressFamily>, Class<? extends SubsequentAddressFamily>, NextHopParser>
        parserHandler = HashBasedTable.create();
    private final Map<Class<? extends CNextHop>, NextHopSerializer> serializerHandler = new HashMap<>();

    @Override
    public CNextHop parseNextHop(final Class<? extends AddressFamily> afi, final Class<? extends
        SubsequentAddressFamily> safi, final ByteBuf bytes) throws BGPParsingException {
        NextHopParser nextHopParser = this.parserHandler.get(afi, safi);
        if (nextHopParser == null && (bytes.readableBytes() == Ipv6Util.IPV6_LENGTH ||
            bytes.readableBytes() == Ipv6Util.IPV6_LENGTH * 2)) {
            nextHopParser = this.parserHandler.get(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
        } else if (nextHopParser == null && bytes.readableBytes() == Ipv4Util.IP4_LENGTH) {
            nextHopParser = this.parserHandler.get(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        }

        if (nextHopParser == null) {
            return null;
        }
        return nextHopParser.parseNextHop(bytes);
    }

    @Override
    public void serializeNextHop(final CNextHop cNextHop, final ByteBuf bytes) {
        if (cNextHop == null) {
            return;
        }
        final NextHopSerializer serializer = this.serializerHandler.get(cNextHop.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeNextHop(cNextHop, bytes);
    }

    void registerNextHopParser(final Class<? extends AddressFamily> afi, final Class<? extends
        SubsequentAddressFamily> safi, final NextHopParser parser) {
        Preconditions.checkNotNull(afi);
        Preconditions.checkNotNull(safi);
        final NextHopParser nextHopParser = this.parserHandler.get(afi, safi);
        if (nextHopParser != null) {
            LOG.debug("AFI/SAFI is already bound to parser {}, overriding it with parser {}", nextHopParser, parser);
        }
        this.parserHandler.put(afi, safi, parser);
    }

    void registerNextHopSerializer(Class<? extends CNextHop> cNextHop, final NextHopSerializer serializer) {
        final NextHopSerializer nextHopSerializer = this.serializerHandler.get(cNextHop);
        if (nextHopSerializer != null) {
            LOG.debug("AFI/SAFI is already bound to parser {}, overriding it with parser {}", nextHopSerializer, serializer);
        }
        this.serializerHandler.put(cNextHop, serializer);
    }
}
