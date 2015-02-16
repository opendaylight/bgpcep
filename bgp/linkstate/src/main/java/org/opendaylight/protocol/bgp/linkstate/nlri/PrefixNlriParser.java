/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.PrefixDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PrefixNlriParser {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixNlriParser.class);

    /* Prefix Descriptor TLVs */
    private static final int OSPF_ROUTE_TYPE = 264;
    private static final int IP_REACHABILITY = 265;

    static PrefixDescriptors parsePrefixDescriptors(final ByteBuf buffer, final boolean ipv4) throws BGPParsingException {
        final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing Prefix Descriptor: {}", ByteBufUtil.hexDump(value));
            }
            switch (type) {
            case TlvUtil.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
                builder.setMultiTopologyId(topologyId);
                LOG.trace("Parsed Topology Identifier: {}", topologyId);
                break;
            case OSPF_ROUTE_TYPE:
                final int rt = value.readByte();
                final OspfRouteType routeType = OspfRouteType.forValue(rt);
                if (routeType == null) {
                    throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
                }
                builder.setOspfRouteType(routeType);
                LOG.trace("Parser RouteType: {}", routeType);
                break;
            case IP_REACHABILITY:
                IpPrefix prefix = null;
                final int prefixLength = value.readByte();
                final int size = prefixLength / Byte.SIZE + ((prefixLength % Byte.SIZE == 0) ? 0 : 1);
                if (size != value.readableBytes()) {
                    LOG.debug("Expected length {}, actual length {}.", size, value.readableBytes());
                    throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.readableBytes()));
                }
                prefix = (ipv4) ? new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(value, size), prefixLength)):
                    new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(value, size), prefixLength));
                builder.setIpReachabilityInformation(prefix);
                LOG.trace("Parsed IP reachability info: {}", prefix);
                break;
            default:
                throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
            }
        }
        LOG.debug("Finished parsing Prefix descriptors.");
        return builder.build();
    }

    static void serializePrefixDescriptors(final PrefixDescriptors descriptors, final ByteBuf buffer) {
        if (descriptors.getMultiTopologyId() != null) {
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(descriptors.getMultiTopologyId().getValue()), buffer);
        }
        if (descriptors.getOspfRouteType() != null) {
            TlvUtil.writeTLV(OSPF_ROUTE_TYPE,
                Unpooled.wrappedBuffer(new byte[] {UnsignedBytes.checkedCast(descriptors.getOspfRouteType().getIntValue()) }), buffer);
        }
        if (descriptors.getIpReachabilityInformation() != null) {
            final IpPrefix prefix = descriptors.getIpReachabilityInformation();
            byte[] prefixBytes = null;
            if (prefix.getIpv4Prefix() != null) {
                prefixBytes = Ipv4Util.bytesForPrefixBegin(prefix.getIpv4Prefix());
            } else if (prefix.getIpv6Prefix() != null) {
                prefixBytes = Ipv6Util.bytesForPrefixBegin(prefix.getIpv6Prefix());
            }
            TlvUtil.writeTLV(IP_REACHABILITY, Unpooled.wrappedBuffer(prefixBytes), buffer);
        }
    }
}
