/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LinkNlriParser {

    private static final Logger LOG = LoggerFactory.getLogger(LinkNlriParser.class);

    /* Link Descriptor TLVs */
    private static final int LINK_LR_IDENTIFIERS = 258;
    private static final int IPV4_IFACE_ADDRESS = 259;
    private static final int IPV4_NEIGHBOR_ADDRESS = 260;
    private static final int IPV6_IFACE_ADDRESS = 261;
    private static final int IPV6_NEIGHBOR_ADDRESS = 262;

    static LinkDescriptors parseLinkDescriptors(final ByteBuf buffer) throws BGPParsingException {
        final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.slice(buffer.readerIndex(), length);
            LOG.trace("Parsing Link Descriptor: {}", ByteBufUtil.hexDump(value));
            switch (type) {
            case LINK_LR_IDENTIFIERS:
                builder.setLinkLocalIdentifier(value.readUnsignedInt());
                builder.setLinkRemoteIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed link local {} remote {} Identifiers.", builder.getLinkLocalIdentifier(),
                    builder.getLinkRemoteIdentifier());
                break;
            case IPV4_IFACE_ADDRESS:
                final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setIpv4InterfaceAddress(lipv4);
                LOG.debug("Parsed IPv4 interface address {}.", lipv4);
                break;
            case IPV4_NEIGHBOR_ADDRESS:
                final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setIpv4NeighborAddress(ripv4);
                LOG.debug("Parsed IPv4 neighbor address {}.", ripv4);
                break;
            case IPV6_IFACE_ADDRESS:
                final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setIpv6InterfaceAddress(lipv6);
                LOG.debug("Parsed IPv6 interface address {}.", lipv6);
                break;
            case IPV6_NEIGHBOR_ADDRESS:
                final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setIpv6NeighborAddress(ripv6);
                LOG.debug("Parsed IPv6 neighbor address {}.", ripv6);
                break;
            case TlvUtil.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topId = new TopologyIdentifier(value.readUnsignedShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
                builder.setMultiTopologyId(topId);
                LOG.debug("Parsed topology identifier {}.", topId);
                break;
            default:
                throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
            }
            buffer.skipBytes(length);
        }
        LOG.trace("Finished parsing Link descriptors.");
        return builder.build();
    }

    static void serializeLinkDescriptors(final LinkDescriptors descriptors, final ByteBuf buffer) {
        if (descriptors.getLinkLocalIdentifier() != null && descriptors.getLinkRemoteIdentifier() != null) {
            final ByteBuf identifierBuf = Unpooled.buffer();
            identifierBuf.writeInt(descriptors.getLinkLocalIdentifier().intValue());
            identifierBuf.writeInt(descriptors.getLinkRemoteIdentifier().intValue());
            TlvUtil.writeTLV(LINK_LR_IDENTIFIERS, identifierBuf, buffer);
        }
        if (descriptors.getIpv4InterfaceAddress() != null) {
            TlvUtil.writeTLV(IPV4_IFACE_ADDRESS, Ipv4Util.byteBufForAddress(descriptors.getIpv4InterfaceAddress()), buffer);
        }
        if (descriptors.getIpv4NeighborAddress() != null) {
            TlvUtil.writeTLV(IPV4_NEIGHBOR_ADDRESS, Ipv4Util.byteBufForAddress(descriptors.getIpv4NeighborAddress()), buffer);
        }
        if (descriptors.getIpv6InterfaceAddress() != null) {
            TlvUtil.writeTLV(IPV6_IFACE_ADDRESS, Ipv6Util.byteBufForAddress(descriptors.getIpv6InterfaceAddress()), buffer);
        }
        if (descriptors.getIpv6NeighborAddress() != null) {
            TlvUtil.writeTLV(IPV6_NEIGHBOR_ADDRESS, Ipv6Util.byteBufForAddress(descriptors.getIpv6NeighborAddress()), buffer);
        }
        if (descriptors.getMultiTopologyId() != null) {
            TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(descriptors.getMultiTopologyId().getValue()), buffer);
        }
    }
}
