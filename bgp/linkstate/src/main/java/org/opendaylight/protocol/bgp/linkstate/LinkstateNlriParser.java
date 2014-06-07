/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriUtil;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser and serializer for Linkstate NLRI.
 */
public final class LinkstateNlriParser implements NlriParser {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateNlriParser.class);
    private static final int ROUTE_DISTINGUISHER_LENGTH = 8;
    private static final int PROTOCOL_ID_LENGTH = 1;
    private static final int IDENTIFIER_LENGTH = 8;
    private static final int LINK_IDENTIFIER_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;

    private static final int TYPE_LENGTH = 2;
    private static final int LENGTH_SIZE = 2;

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
            final byte[] value = ByteArray.readBytes(buffer, length);
            LOG.trace("Parsing Link Descriptor: {}", Arrays.toString(value));
            switch (type) {
            case TlvCode.LINK_LR_IDENTIFIERS:
                builder.setLinkLocalIdentifier(ByteArray.bytesToUint32(ByteArray.subByte(value, 0, LINK_IDENTIFIER_LENGTH)).longValue());
                builder.setLinkRemoteIdentifier(ByteArray.bytesToUint32(
                        ByteArray.subByte(value, LINK_IDENTIFIER_LENGTH, LINK_IDENTIFIER_LENGTH)).longValue());
                LOG.debug("Parsed link local {} remote {} Identifiers.", builder.getLinkLocalIdentifier(),
                        builder.getLinkRemoteIdentifier());
                break;
            case TlvCode.IPV4_IFACE_ADDRESS:
                final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
                builder.setIpv4InterfaceAddress(lipv4);
                LOG.debug("Parsed IPv4 interface address {}.", lipv4);
                break;
            case TlvCode.IPV4_NEIGHBOR_ADDRESS:
                final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForBytes(value));
                builder.setIpv4NeighborAddress(ripv4);
                LOG.debug("Parsed IPv4 neighbor address {}.", ripv4);
                break;
            case TlvCode.IPV6_IFACE_ADDRESS:
                final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
                builder.setIpv6InterfaceAddress(lipv6);
                LOG.debug("Parsed IPv6 interface address {}.", lipv6);
                break;
            case TlvCode.IPV6_NEIGHBOR_ADDRESS:
                final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForBytes(value));
                builder.setIpv6NeighborAddress(ripv6);
                LOG.debug("Parsed IPv6 neighbor address {}.", ripv6);
                break;
            case TlvCode.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
                builder.setMultiTopologyId(topId);
                LOG.debug("Parsed topology identifier {}.", topId);
                break;
            default:
                throw new BGPParsingException("Link Descriptor not recognized, type: " + type);
            }
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
            final byte[] value = ByteArray.readBytes(buffer, length);
            LOG.trace("Parsing Node Descriptor: {}", Arrays.toString(value));
            switch (type) {
            case TlvCode.AS_NUMBER:
                asnumber = new AsNumber(ByteArray.bytesToLong(value));
                LOG.debug("Parsed {}", asnumber);
                break;
            case TlvCode.BGP_LS_ID:
                bgpId = new DomainIdentifier(UnsignedInteger.fromIntBits(ByteArray.bytesToInt(value)).longValue());
                LOG.debug("Parsed {}", bgpId);
                break;
            case TlvCode.AREA_ID:
                ai = new AreaIdentifier(UnsignedInteger.fromIntBits(ByteArray.bytesToInt(value)).longValue());
                LOG.debug("Parsed area identifier {}", ai);
                break;
            case TlvCode.IGP_ROUTER_ID:
                routerId = parseRouterId(value);
                LOG.debug("Parsed Router Identifier {}", routerId);
                break;
            default:
                throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
            }
        }
        LOG.trace("Finished parsing Node descriptors.");
        return (local) ? new LocalNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(
                routerId).build()
                : new RemoteNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(routerId).build();
    }

    private static CRouterIdentifier parseRouterId(final byte[] value) throws BGPParsingException {
        if (value.length == ISO_SYSTEM_ID_LENGTH) {
            return new IsisNodeCaseBuilder().setIsisNode(
                    new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, ISO_SYSTEM_ID_LENGTH))).build()).build();
        }
        if (value.length == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH) {
            if (value[ISO_SYSTEM_ID_LENGTH] == 0) {
                LOG.warn("PSN octet is 0. Ignoring System ID.");
                return new IsisNodeCaseBuilder().setIsisNode(
                        new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.subByte(value, 0, ISO_SYSTEM_ID_LENGTH))).build()).build();
            } else {
                final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
                        new IsoSystemIdentifier(ByteArray.subByte(value, 0, ISO_SYSTEM_ID_LENGTH))).build();
                return new IsisPseudonodeCaseBuilder().setIsisPseudonode(
                        new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn(
                                (short) UnsignedBytes.toInt(value[ISO_SYSTEM_ID_LENGTH])).build()).build();
            }
        }
        if (value.length == 4) {
            return new OspfNodeCaseBuilder().setOspfNode(
                    new OspfNodeBuilder().setOspfRouterId(ByteArray.bytesToUint32(value).longValue()).build()).build();
        }
        if (value.length == 8) {
            final byte[] o = ByteArray.subByte(value, 0, 4);
            final OspfInterfaceIdentifier a = new OspfInterfaceIdentifier(ByteArray.bytesToUint32(ByteArray.subByte(value, 4, 4)).longValue());
            return new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                    new OspfPseudonodeBuilder().setOspfRouterId(ByteArray.bytesToUint32(o).longValue()).setLanInterface(a).build()).build();
        }
        throw new BGPParsingException("Router Id of invalid length " + value.length);
    }

    private static PrefixDescriptors parsePrefixDescriptors(final ByteBuf buffer, final boolean ipv4) throws BGPParsingException {
        final PrefixDescriptorsBuilder builder = new PrefixDescriptorsBuilder();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final byte[] value = ByteArray.readBytes(buffer, length);
            LOG.trace("Parsing Prefix Descriptor: {}", Arrays.toString(value));
            switch (type) {
            case TlvCode.MULTI_TOPOLOGY_ID:
                final TopologyIdentifier topologyId = new TopologyIdentifier(ByteArray.bytesToInt(value) & 0x3fff);
                builder.setMultiTopologyId(topologyId);
                LOG.trace("Parsed Topology Identifier: {}", topologyId);
                break;
            case TlvCode.OSPF_ROUTE_TYPE:
                final int rt = ByteArray.bytesToInt(value);
                final OspfRouteType routeType = OspfRouteType.forValue(rt);
                if (routeType == null) {
                    throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
                }
                builder.setOspfRouteType(routeType);
                LOG.trace("Parser RouteType: {}", routeType);
                break;
            case TlvCode.IP_REACHABILITY:
                IpPrefix prefix = null;
                final int prefixLength = UnsignedBytes.toInt(value[0]);
                final int size = prefixLength / Byte.SIZE + ((prefixLength % Byte.SIZE == 0) ? 0 : 1);
                if (size != value.length - 1) {
                    LOG.debug("Expected length {}, actual length {}.", size, value.length - 1);
                    throw new BGPParsingException("Illegal length of IP reachability TLV: " + (value.length - 1));
                }
                if (ipv4) {
                    prefix = new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
                } else {
                    prefix = new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(value, 1, size), prefixLength));
                }
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

    /**
     * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
     *
     * @param nlri as byte array
     * @return {@link CLinkstateDestination}
     * @throws BGPParsingException if parsing was unsuccessful
     */
    private List<CLinkstateDestination> parseNlri(final ByteBuf nlri) throws BGPParsingException {
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
            if (this.isVpn) {
                // this parses route distinguisher
                distinguisher = new RouteDistinguisher(BigInteger.valueOf(nlri.readLong()));
                builder.setDistinguisher(distinguisher);
            }
            // parse source protocol
            final ProtocolId sp = ProtocolId.forValue(UnsignedBytes.toInt(nlri.readByte()));
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
            final int restLength = length - (this.isVpn ? ROUTE_DISTINGUISHER_LENGTH : 0) - PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH
                    - TYPE_LENGTH - LENGTH_SIZE - locallength;
            LOG.trace("Restlength {}", restLength);
            ByteBuf rest = nlri.slice(nlri.readerIndex(), restLength);
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
        final List<CLinkstateDestination> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                                dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final byte[] nextHop, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                        new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build()).build());
        NlriUtil.parseNextHop(nextHop, builder);
    }

    /**
     * Serializes Linkstate NLRI to byte array. We need this as NLRI serves as a key in upper layers.
     *
     * @param destination Linkstate NLRI to be serialized
     * @return byte array
     */
    public static byte[] serializeNlri(final CLinkstateDestination destination) {
        final ByteBuf finalBuffer = Unpooled.buffer();
        finalBuffer.writeShort(destination.getNlriType().getIntValue());
        final ByteBuf buffer = Unpooled.buffer();
        if (destination.getDistinguisher() != null) {
            buffer.writeBytes(destination.getDistinguisher().getValue().toByteArray());
        }
        buffer.writeByte(destination.getProtocolId().getIntValue());
        buffer.writeBytes(ByteArray.longToBytes(destination.getIdentifier().getValue().longValue(), IDENTIFIER_LENGTH));

        // serialize local node descriptors
        final byte[] ldescs = serializeNodeDescriptors(destination.getLocalNodeDescriptors());
        buffer.writeShort(TlvCode.LOCAL_NODE_DESCRIPTORS);
        buffer.writeShort(ldescs.length);
        buffer.writeBytes(ldescs);

        switch (destination.getNlriType()) {
        case Ipv4Prefix:
        case Ipv6Prefix:
            if (destination.getPrefixDescriptors() != null) {
                serializePrefixDescriptors(buffer, destination.getPrefixDescriptors());
            }
            break;
        case Link:
            final byte[] rdescs = serializeNodeDescriptors(destination.getRemoteNodeDescriptors());
            buffer.writeShort(TlvCode.REMOTE_NODE_DESCRIPTORS);
            buffer.writeShort(rdescs.length);
            buffer.writeBytes(rdescs);
            if (destination.getLinkDescriptors() != null) {
                serializeLinkDescriptors(buffer, destination.getLinkDescriptors());
            }
            break;
        case Node:
            break;
        default:
            LOG.warn("Unknown NLRI Type.");
            break;
        }
        finalBuffer.writeShort(buffer.readableBytes());
        finalBuffer.writeBytes(buffer);
        return ByteArray.subByte(finalBuffer.array(), 0, finalBuffer.readableBytes());
    }

    private static byte[] serializeNodeDescriptors(final NodeIdentifier descriptors) {
        final ByteBuf buffer = Unpooled.buffer();
        final int length = Integer.SIZE / Byte.SIZE;
        if (descriptors.getAsNumber() != null) {
            buffer.writeShort(TlvCode.AS_NUMBER);
            buffer.writeShort(length);
            buffer.writeInt(UnsignedInteger.valueOf(descriptors.getAsNumber().getValue()).intValue());
        }
        if (descriptors.getDomainId() != null) {
            buffer.writeShort(TlvCode.BGP_LS_ID);
            buffer.writeShort(length);
            buffer.writeInt(UnsignedInteger.valueOf(descriptors.getDomainId().getValue()).intValue());
        }
        if (descriptors.getAreaId() != null) {
            buffer.writeShort(TlvCode.AREA_ID);
            buffer.writeShort(length);
            buffer.writeInt(UnsignedInteger.valueOf(descriptors.getAreaId().getValue()).intValue());
        }
        if (descriptors.getCRouterIdentifier() != null) {
            final byte[] value = serializeRouterId(descriptors.getCRouterIdentifier());
            buffer.writeShort(TlvCode.IGP_ROUTER_ID);
            buffer.writeShort(value.length);
            buffer.writeBytes(value);
        }
        return ByteArray.readAllBytes(buffer);
    }

    private static byte[] serializeRouterId(final CRouterIdentifier routerId) {
        byte[] bytes = null;
        if (routerId instanceof IsisNodeCase) {
            final IsisNode isis = ((IsisNodeCase) routerId).getIsisNode();
            bytes = isis.getIsoSystemId().getValue();
        } else if (routerId instanceof IsisPseudonodeCase) {
            bytes = new byte[ISO_SYSTEM_ID_LENGTH + PSN_LENGTH];
            final IsisPseudonode isis = ((IsisPseudonodeCase) routerId).getIsisPseudonode();
            ByteArray.copyWhole(isis.getIsIsRouterIdentifier().getIsoSystemId().getValue(), bytes, 0);
            bytes[6] = UnsignedBytes.checkedCast((isis.getPsn() != null) ? isis.getPsn() : 0);
        } else if (routerId instanceof OspfNodeCase) {
            bytes = ByteArray.uint32ToBytes(((OspfNodeCase) routerId).getOspfNode().getOspfRouterId());
        } else if (routerId instanceof OspfPseudonodeCase) {
            final OspfPseudonode node = ((OspfPseudonodeCase) routerId).getOspfPseudonode();
            bytes = new byte[2 * Integer.SIZE / Byte.SIZE];
            ByteArray.copyWhole(ByteArray.uint32ToBytes(node.getOspfRouterId()), bytes, 0);
            ByteArray.copyWhole(ByteArray.uint32ToBytes(node.getLanInterface().getValue()), bytes, Integer.SIZE / Byte.SIZE);
        }
        return bytes;
    }

    private static void serializeLinkDescriptors(final ByteBuf buffer, final LinkDescriptors descriptors) {
        if (descriptors.getLinkLocalIdentifier() != null && descriptors.getLinkRemoteIdentifier() != null) {
            buffer.writeShort(TlvCode.LINK_LR_IDENTIFIERS);
            buffer.writeShort(LINK_IDENTIFIER_LENGTH);
            buffer.writeInt(UnsignedInteger.valueOf(descriptors.getLinkLocalIdentifier()).intValue());
            buffer.writeInt(UnsignedInteger.valueOf(descriptors.getLinkRemoteIdentifier()).intValue());
        }
        if (descriptors.getIpv4InterfaceAddress() != null) {
            final byte[] ipv4Address = Ipv4Util.bytesForAddress(descriptors.getIpv4InterfaceAddress());
            buffer.writeShort(TlvCode.IPV4_IFACE_ADDRESS);
            buffer.writeShort(ipv4Address.length);
            buffer.writeBytes(ipv4Address);
        }
        if (descriptors.getIpv4NeighborAddress() != null) {
            final byte[] ipv4Address = Ipv4Util.bytesForAddress(descriptors.getIpv4NeighborAddress());
            buffer.writeShort(TlvCode.IPV4_NEIGHBOR_ADDRESS);
            buffer.writeShort(ipv4Address.length);
            buffer.writeBytes(ipv4Address);
        }
        if (descriptors.getIpv6InterfaceAddress() != null) {
            final byte[] ipv6Address = Ipv6Util.bytesForAddress(descriptors.getIpv6InterfaceAddress());
            buffer.writeShort(TlvCode.IPV6_IFACE_ADDRESS);
            buffer.writeShort(ipv6Address.length);
            buffer.writeBytes(ipv6Address);
        }
        if (descriptors.getIpv6NeighborAddress() != null) {
            final byte[] ipv6Address = Ipv6Util.bytesForAddress(descriptors.getIpv6NeighborAddress());
            buffer.writeShort(TlvCode.IPV6_NEIGHBOR_ADDRESS);
            buffer.writeShort(ipv6Address.length);
            buffer.writeBytes(ipv6Address);
        }
        if (descriptors.getMultiTopologyId() != null) {
            buffer.writeShort(TlvCode.MULTI_TOPOLOGY_ID);
            buffer.writeShort(Short.SIZE / Byte.SIZE);
            buffer.writeShort(descriptors.getMultiTopologyId().getValue());
        }
    }

    private static void serializePrefixDescriptors(final ByteBuf buffer, final PrefixDescriptors descriptors) {
        if (descriptors.getMultiTopologyId() != null) {
            buffer.writeShort(TlvCode.MULTI_TOPOLOGY_ID);
            buffer.writeShort(Short.SIZE / Byte.SIZE);
            buffer.writeShort(descriptors.getMultiTopologyId().getValue());
        }
        if (descriptors.getOspfRouteType() != null) {
            buffer.writeShort(TlvCode.OSPF_ROUTE_TYPE);
            buffer.writeShort(1);
            buffer.writeByte(descriptors.getOspfRouteType().getIntValue());
        }
        if (descriptors.getIpReachabilityInformation() != null) {
            final IpPrefix prefix = descriptors.getIpReachabilityInformation();
            byte[] prefixBytes = null;
            final int prefixLength = Ipv4Util.getPrefixLength(prefix);
            if (prefix.getIpv4Prefix() != null) {
                prefixBytes = ByteArray.trim(ByteArray.subByte(Ipv4Util.bytesForPrefix(prefix.getIpv4Prefix()), 0,
                        Ipv4Util.bytesForPrefix(prefix.getIpv4Prefix()).length - 1));
            } else if (prefix.getIpv6Prefix() != null) {
                prefixBytes = ByteArray.trim(ByteArray.subByte(Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()), 0,
                        Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()).length - 1));
            }
            buffer.writeShort(TlvCode.IP_REACHABILITY);
            buffer.writeShort(1 + prefixBytes.length);
            buffer.writeByte(prefixLength);
            buffer.writeBytes(prefixBytes);
        }
    }
}
