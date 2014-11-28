/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.PrefixDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser and serializer for Linkstate NLRI.
 */
public final class LinkstateNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateNlriParser.class);
    private static final int ROUTE_DISTINGUISHER_LENGTH = 8;
    private static final int PROTOCOL_ID_LENGTH = 1;
    private static final int IDENTIFIER_LENGTH = 8;
    private static final int OSPF_PSEUDONODE_ROUTER_ID_LENGTH = 8;
    private static final int OSPF_ROUTER_ID_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;

    private static final int TYPE_LENGTH = 2;
    private static final int LENGTH_SIZE = 2;

    static final int TOPOLOGY_ID_OFFSET = 0x3fff;

    private final boolean isVpn;

    public LinkstateNlriParser(final boolean isVpn) {
        this.isVpn = isVpn;
    }

    private static NodeIdentifier parseLink(final CLinkstateDestinationBuilder builder, final ByteBuf buffer) throws BGPParsingException {
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        final NodeIdentifier remote = null;
        if (type == TlvCode.REMOTE_NODE_DESCRIPTORS) {
            builder.setRemoteNodeDescriptors((RemoteNodeDescriptors) parseNodeDescriptors(buffer.slice(buffer.readerIndex(), length), false));
            buffer.skipBytes(length);
        }
        builder.setLinkDescriptors(parseLinkDescriptors(buffer.slice()));
        return remote;
    }

    private static LinkDescriptors parseLinkDescriptors(final ByteBuf buffer) throws BGPParsingException {
        final LinkDescriptorsBuilder builder = new LinkDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.slice(buffer.readerIndex(), length);
            LOG.trace("Parsing Link Descriptor: {}", ByteBufUtil.hexDump(value));
            switch (type) {
            case TlvCode.LINK_LR_IDENTIFIERS:
                builder.setLinkLocalIdentifier(value.readUnsignedInt());
                builder.setLinkRemoteIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed link local {} remote {} Identifiers.", builder.getLinkLocalIdentifier(),
                    builder.getLinkRemoteIdentifier());
                break;
            case TlvCode.IPV4_IFACE_ADDRESS:
                final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setIpv4InterfaceAddress(lipv4);
                LOG.debug("Parsed IPv4 interface address {}.", lipv4);
                break;
            case TlvCode.IPV4_NEIGHBOR_ADDRESS:
                final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
                builder.setIpv4NeighborAddress(ripv4);
                LOG.debug("Parsed IPv4 neighbor address {}.", ripv4);
                break;
            case TlvCode.IPV6_IFACE_ADDRESS:
                final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setIpv6InterfaceAddress(lipv6);
                LOG.debug("Parsed IPv6 interface address {}.", lipv6);
                break;
            case TlvCode.IPV6_NEIGHBOR_ADDRESS:
                final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
                builder.setIpv6NeighborAddress(ripv6);
                LOG.debug("Parsed IPv6 neighbor address {}.", ripv6);
                break;
            case TlvCode.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topId = new TopologyIdentifier(value.readUnsignedShort() & LinkstateNlriParser.TOPOLOGY_ID_OFFSET);
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

    private static NodeIdentifier parseNodeDescriptors(final ByteBuf buffer, final boolean local) throws BGPParsingException {
        AsNumber asnumber = null;
        DomainIdentifier bgpId = null;
        AreaIdentifier ai = null;
        CRouterIdentifier routerId = null;
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.slice(buffer.readerIndex(), length);
            LOG.trace("Parsing Node Descriptor: {}", ByteBufUtil.hexDump(value));
            switch (type) {
            case TlvCode.AS_NUMBER:
                asnumber = new AsNumber(value.readUnsignedInt());
                LOG.debug("Parsed {}", asnumber);
                break;
            case TlvCode.BGP_LS_ID:
                bgpId = new DomainIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed {}", bgpId);
                break;
            case TlvCode.AREA_ID:
                ai = new AreaIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed area identifier {}", ai);
                break;
            case TlvCode.IGP_ROUTER_ID:
                routerId = parseRouterId(value);
                LOG.debug("Parsed Router Identifier {}", routerId);
                break;
            default:
                throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
            }
            buffer.skipBytes(length);
        }
        LOG.trace("Finished parsing Node descriptors.");
        return (local) ? new LocalNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(
            routerId).build()
            : new RemoteNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(routerId).build();
    }

    private static CRouterIdentifier parseRouterId(final ByteBuf value) throws BGPParsingException {
        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH || (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH && value.getByte(ISO_SYSTEM_ID_LENGTH) == 0)) {
            return new IsisNodeCaseBuilder().setIsisNode(
                new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build()).build();
        }
        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH) {
            final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
                new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build();
            return new IsisPseudonodeCaseBuilder().setIsisPseudonode(new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn((short) value.readByte()).build()).build();
        }
        if (value.readableBytes() == OSPF_ROUTER_ID_LENGTH) {
            return new OspfNodeCaseBuilder().setOspfNode(
                new OspfNodeBuilder().setOspfRouterId(value.readUnsignedInt()).build()).build();
        }
        if (value.readableBytes() == OSPF_PSEUDONODE_ROUTER_ID_LENGTH) {
            return new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                new OspfPseudonodeBuilder().setOspfRouterId(value.readUnsignedInt()).setLanInterface(new OspfInterfaceIdentifier(value.readUnsignedInt())).build()).build();
        }
        throw new BGPParsingException("Router Id of invalid length " + value.readableBytes());
    }

    private static PrefixDescriptors parsePrefixDescriptors(final ByteBuf buffer, final boolean ipv4) throws BGPParsingException {
        final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.slice(buffer.readerIndex(), length);
            LOG.trace("Parsing Prefix Descriptor: {}", ByteBufUtil.hexDump(value));
            switch (type) {
            case TlvCode.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & LinkstateNlriParser.TOPOLOGY_ID_OFFSET);
                builder.setMultiTopologyId(topologyId);
                LOG.trace("Parsed Topology Identifier: {}", topologyId);
                break;
            case TlvCode.OSPF_ROUTE_TYPE:
                final int rt = value.readByte();
                final OspfRouteType routeType = OspfRouteType.forValue(rt);
                if (routeType == null) {
                    throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
                }
                builder.setOspfRouteType(routeType);
                LOG.trace("Parser RouteType: {}", routeType);
                break;
            case TlvCode.IP_REACHABILITY:
                IpPrefix prefix = null;
                final int prefixLength = value.readByte();
                final int size = prefixLength / Byte.SIZE + ((prefixLength % Byte.SIZE == 0) ? 0 : 1);
                if (size != value.readableBytes()) {
                    LOG.debug("Expected length {}, actual length {}.", size, value.readableBytes());
                    throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.readableBytes()));
                }
                if (ipv4) {
                    prefix = new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.readBytes(value, size), prefixLength));
                } else {
                    prefix = new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.readBytes(value, size), prefixLength));
                }
                builder.setIpReachabilityInformation(prefix);
                LOG.trace("Parsed IP reachability info: {}", prefix);
                break;
            default:
                throw new BGPParsingException("Prefix Descriptor not recognized, type: " + type);
            }
            buffer.skipBytes(length);
        }
        LOG.debug("Finished parsing Prefix descriptors.");
        return builder.build();
    }

    /**
     * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
     *
     * @param nlri as byte array
     * @return {@link CLinkstateDestination}
     * @throws BGPParsingException if parsing was unsuccessful
     */
    public static List<CLinkstateDestination> parseNlri(final ByteBuf nlri, final boolean isVpn) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<CLinkstateDestination> dests = Lists.newArrayList();

        CLinkstateDestinationBuilder builder = null;

        while (nlri.isReadable()) {
            builder = new CLinkstateDestinationBuilder();
            final NlriType type = NlriType.forValue(nlri.readUnsignedShort());
            builder.setNlriType(type);

            // length means total length of the tlvs including route distinguisher not including the type field
            final int length = nlri.readUnsignedShort();
            RouteDistinguisher distinguisher = null;
            if (isVpn) {
                // this parses route distinguisher
                distinguisher = new RouteDistinguisher(BigInteger.valueOf(nlri.readLong()));
                builder.setDistinguisher(distinguisher);
            }
            // parse source protocol
            final ProtocolId sp = ProtocolId.forValue(nlri.readByte());
            builder.setProtocolId(sp);

            // parse identifier
            final Identifier identifier = new Identifier(BigInteger.valueOf(nlri.readLong()));
            builder.setIdentifier(identifier);

            // if we are dealing with linkstate nodes/links, parse local node descriptor
            NodeIdentifier localDescriptor = null;
            int locallength = 0;
            final int localtype = nlri.readUnsignedShort();
            locallength = nlri.readUnsignedShort();
            if (localtype == TlvCode.LOCAL_NODE_DESCRIPTORS) {
                localDescriptor = parseNodeDescriptors(nlri.slice(nlri.readerIndex(), locallength), true);
            }
            nlri.skipBytes(locallength);
            builder.setLocalNodeDescriptors((LocalNodeDescriptors) localDescriptor);
            final int restLength = length - (isVpn ? ROUTE_DISTINGUISHER_LENGTH : 0) - PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH
                - TYPE_LENGTH - LENGTH_SIZE - locallength;
            LOG.trace("Restlength {}", restLength);
            final ByteBuf rest = nlri.slice(nlri.readerIndex(), restLength);
            switch (type) {
            case Link:
                parseLink(builder, rest);
                break;
            case Ipv4Prefix:
                builder.setPrefixDescriptors(parsePrefixDescriptors(rest, true));
                break;
            case Ipv6Prefix:
                builder.setPrefixDescriptors(parsePrefixDescriptors(rest, false));
                break;
            case Node:
                // node nlri is already parsed as it contains only the common fields for node and link nlri
                break;
            default:
                break;
            }
            nlri.skipBytes(restLength);
            dests.add(builder.build());
        }
        return dests;
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri, this.isVpn);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri, this.isVpn);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build()).build());
    }

    /**
     * Serializes Linkstate NLRI to byte array. We need this as NLRI serves as a key in upper layers.
     *
     * @param destination Linkstate NLRI to be serialized
     */
    public static void serializeNlri(final CLinkstateDestination destination, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        if (destination.getDistinguisher() != null) {
            nlriByteBuf.writeBytes(destination.getDistinguisher().getValue().toByteArray());
        }
        nlriByteBuf.writeByte(destination.getProtocolId().getIntValue());
        nlriByteBuf.writeLong(destination.getIdentifier().getValue().longValue());

        // serialize local node descriptors
        final ByteBuf ldescs = Unpooled.buffer();
        serializeNodeDescriptors(destination.getLocalNodeDescriptors(), ldescs);
        LinkstateAttributeParser.writeTLV(TlvCode.LOCAL_NODE_DESCRIPTORS, ldescs, nlriByteBuf);

        switch (destination.getNlriType()) {
        case Ipv4Prefix:
        case Ipv6Prefix:
            if (destination.getPrefixDescriptors() != null) {
                serializePrefixDescriptors(destination.getPrefixDescriptors(), nlriByteBuf);
            }
            break;
        case Link:
            final ByteBuf rdescs = Unpooled.buffer();
            serializeNodeDescriptors(destination.getRemoteNodeDescriptors(), rdescs);
            LinkstateAttributeParser.writeTLV(TlvCode.REMOTE_NODE_DESCRIPTORS, rdescs, nlriByteBuf);
            if (destination.getLinkDescriptors() != null) {
                serializeLinkDescriptors(destination.getLinkDescriptors(), nlriByteBuf);
            }
            break;
        case Node:
            break;
        default:
            LOG.warn("Unknown NLRI Type.");
            break;
        }
        LinkstateAttributeParser.writeTLV(destination.getNlriType().getIntValue(), nlriByteBuf, buffer);
    }

    private static void serializeNodeDescriptors(final NodeIdentifier descriptors, final ByteBuf buffer) {
        if (descriptors.getAsNumber() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getAsNumber().getValue()).intValue()), buffer);
        }
        if (descriptors.getDomainId() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getDomainId().getValue()).intValue()), buffer);
        }
        if (descriptors.getAreaId() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getAreaId().getValue()).intValue()), buffer);
        }
        if (descriptors.getCRouterIdentifier() != null) {
            final ByteBuf routerIdBuf = Unpooled.buffer();
            serializeRouterId(descriptors.getCRouterIdentifier(), routerIdBuf);
            LinkstateAttributeParser.writeTLV(TlvCode.IGP_ROUTER_ID, routerIdBuf, buffer);
        }
    }

    private static void serializeRouterId(final CRouterIdentifier routerId, final ByteBuf buffer) {
        if (routerId instanceof IsisNodeCase) {
            final IsisNode isis = ((IsisNodeCase) routerId).getIsisNode();
            buffer.writeBytes(isis.getIsoSystemId().getValue());
        } else if (routerId instanceof IsisPseudonodeCase) {
            final IsisPseudonode isis = ((IsisPseudonodeCase) routerId).getIsisPseudonode();
            buffer.writeBytes(isis.getIsIsRouterIdentifier().getIsoSystemId().getValue());
            buffer.writeByte(((isis.getPsn() != null) ? isis.getPsn() : 0));
        } else if (routerId instanceof OspfNodeCase) {
            buffer.writeInt(UnsignedInteger.valueOf(((OspfNodeCase) routerId).getOspfNode().getOspfRouterId()).intValue());
        } else if (routerId instanceof OspfPseudonodeCase) {
            final OspfPseudonode node = ((OspfPseudonodeCase) routerId).getOspfPseudonode();
            buffer.writeInt(UnsignedInteger.valueOf(node.getOspfRouterId()).intValue());
            buffer.writeInt(UnsignedInteger.valueOf(node.getLanInterface().getValue()).intValue());
        }
    }

    private static void serializeLinkDescriptors(final LinkDescriptors descriptors, final ByteBuf buffer) {
        if (descriptors.getLinkLocalIdentifier() != null && descriptors.getLinkRemoteIdentifier() != null) {
            final ByteBuf identifierBuf = Unpooled.buffer();
            identifierBuf.writeInt(descriptors.getLinkLocalIdentifier().intValue());
            identifierBuf.writeInt(descriptors.getLinkRemoteIdentifier().intValue());
            LinkstateAttributeParser.writeTLV(TlvCode.LINK_LR_IDENTIFIERS, identifierBuf, buffer);
        }
        if (descriptors.getIpv4InterfaceAddress() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.IPV4_IFACE_ADDRESS, Unpooled.wrappedBuffer(Ipv4Util.bytesForAddress(descriptors.getIpv4InterfaceAddress())), buffer);
        }
        if (descriptors.getIpv4NeighborAddress() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.IPV4_NEIGHBOR_ADDRESS, Unpooled.wrappedBuffer(Ipv4Util.bytesForAddress(descriptors.getIpv4NeighborAddress())), buffer);
        }
        if (descriptors.getIpv6InterfaceAddress() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.IPV6_IFACE_ADDRESS, Unpooled.wrappedBuffer(Ipv6Util.bytesForAddress(descriptors.getIpv6InterfaceAddress())), buffer);
        }
        if (descriptors.getIpv6NeighborAddress() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.IPV6_NEIGHBOR_ADDRESS, Unpooled.wrappedBuffer(Ipv6Util.bytesForAddress(descriptors.getIpv6NeighborAddress())), buffer);
        }
        if (descriptors.getMultiTopologyId() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.MULTI_TOPOLOGY_ID, Unpooled.copyShort(descriptors.getMultiTopologyId().getValue()), buffer);
        }
    }

    private static void serializePrefixDescriptors(final PrefixDescriptors descriptors, final ByteBuf buffer) {
        if (descriptors.getMultiTopologyId() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.MULTI_TOPOLOGY_ID, Unpooled.copyShort(descriptors.getMultiTopologyId().getValue()), buffer);
        }
        if (descriptors.getOspfRouteType() != null) {
            LinkstateAttributeParser.writeTLV(TlvCode.OSPF_ROUTE_TYPE,
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
            LinkstateAttributeParser.writeTLV(TlvCode.IP_REACHABILITY, Unpooled.wrappedBuffer(prefixBytes), buffer);
        }
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes pathAttributes = (PathAttributes) attribute;
        final PathAttributes1 pathAttributes1 = pathAttributes.getAugmentation(PathAttributes1.class);
        final PathAttributes2 pathAttributes2 = pathAttributes.getAugmentation(PathAttributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null &&
                routes.getDestinationType()
                instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase
                linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) routes.getDestinationType();

                for (final CLinkstateDestination cLinkstateDestination : linkstateCase.getDestinationLinkstate().getCLinkstateDestination()) {
                    serializeNlri(cLinkstateDestination, byteAggregator);
                }
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationLinkstateCase) {
                final DestinationLinkstateCase linkstateCase = (DestinationLinkstateCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                for (final CLinkstateDestination cLinkstateDestination : linkstateCase.getDestinationLinkstate().getCLinkstateDestination()) {
                    serializeNlri(cLinkstateDestination, byteAggregator);
                }
            }
        }
    }
}
