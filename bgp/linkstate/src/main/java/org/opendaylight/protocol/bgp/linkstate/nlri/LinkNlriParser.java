/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class LinkNlriParser {

    private static final Logger LOG = LoggerFactory.getLogger(LinkNlriParser.class);

    private LinkNlriParser() {
        throw new UnsupportedOperationException();
    }
    /* Link Descriptor TLVs */
    private static final int LINK_LR_IDENTIFIERS = 258;
    private static final int IPV4_IFACE_ADDRESS = 259;
    private static final int IPV4_NEIGHBOR_ADDRESS = 260;
    private static final int IPV6_IFACE_ADDRESS = 261;
    private static final int IPV6_NEIGHBOR_ADDRESS = 262;

    /* Link Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier IPV4_IFACE_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "ipv4-interface-address")));
    private static final NodeIdentifier IPV6_IFACE_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "ipv6-interface-address")));
    @VisibleForTesting
    public static final NodeIdentifier IPV4_NEIGHBOR_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "ipv4-neighbor-address")));
    private static final NodeIdentifier IPV6_NEIGHBOR_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "ipv6-neighbor-address")));
    @VisibleForTesting
    public static final NodeIdentifier LINK_LOCAL_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "link-local-identifier")));
    @VisibleForTesting
    public static final NodeIdentifier LINK_REMOTE_NID = new NodeIdentifier(QName.cachedReference(QName.create(LinkDescriptors.QNAME, "link-remote-identifier")));
    @VisibleForTesting
    public static final NodeIdentifier BGP_ROUTER_NID = new NodeIdentifier(QName.cachedReference(QName.create(NodeDescriptors.QNAME, "bgp-router-id")));
    @VisibleForTesting
    public static final NodeIdentifier MEMBER_ASN_NID = new NodeIdentifier(QName.cachedReference(QName.create(NodeDescriptors.QNAME, "member-asn")));

    static LinkDescriptors parseLinkDescriptors(final ByteBuf buffer) throws BGPParsingException {
        final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
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
            case TlvUtil.BGP_ROUTER_ID:
                final Ipv4Address bgpRouterId = Ipv4Util.addressForByteBuf(value);
                builder.setBgpRouterId(bgpRouterId);
                LOG.debug("Parsed BGP Router identifier {}.", bgpRouterId);
                break;
            case TlvUtil.MEMBER_AS_NUMBER:
                final AsNumber memberAsn = new AsNumber(value.readUnsignedInt());
                builder.setMemberAsn(memberAsn);
                LOG.debug("Parsed Member AsNumber {}", memberAsn);
                break;
            default:
                throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
            }
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
        if (descriptors.getBgpRouterId() != null) {
            TlvUtil.writeTLV(TlvUtil.BGP_ROUTER_ID, Ipv4Util.byteBufForAddress((descriptors.getBgpRouterId())), buffer);
        }
        if (descriptors.getMemberAsn() != null) {
            TlvUtil.writeTLV(TlvUtil.MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getMemberAsn().getValue()).intValue()), buffer);
        }
    }

    static LinkDescriptors serializeLinkDescriptors(final ContainerNode descriptors) {
        final LinkDescriptorsBuilder linkDescBuilder = new LinkDescriptorsBuilder();

        if (descriptors.getChild(LINK_LOCAL_NID).isPresent() && descriptors.getChild(LINK_REMOTE_NID).isPresent()) {
            linkDescBuilder.setLinkLocalIdentifier((Long) descriptors.getChild(LINK_LOCAL_NID).get().getValue());
            linkDescBuilder.setLinkRemoteIdentifier((Long) descriptors.getChild(LINK_REMOTE_NID).get().getValue());
        }
        if (descriptors.getChild(IPV4_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv6InterfaceAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV4_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv4NeighborAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv6NeighborAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).isPresent()) {
            linkDescBuilder.setMultiTopologyId(new TopologyIdentifier((Integer) descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).get().getValue()));
        }
        if (descriptors.getChild(TlvUtil.BGP_ROUTER_NID).isPresent()) {
            linkDescBuilder.setBgpRouterId(new Ipv4Address((String) descriptors.getChild(TlvUtil.BGP_ROUTER_NID).get().getValue()));
        }
        if (descriptors.getChild(TlvUtil.MEMBER_ASN_NID).isPresent()) {
            linkDescBuilder.setMemberAsn(new AsNumber((Long) descriptors.getChild(TlvUtil.MEMBER_ASN_NID).get().getValue()));
        }
        return linkDescBuilder.build();
    }
}
