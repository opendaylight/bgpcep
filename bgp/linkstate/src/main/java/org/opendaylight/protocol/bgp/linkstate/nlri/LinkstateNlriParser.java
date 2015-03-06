/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
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

    private static final int TYPE_LENGTH = 2;
    private static final int LENGTH_SIZE = 2;


    private static final int LOCAL_NODE_DESCRIPTORS = 256;
    private static final int REMOTE_NODE_DESCRIPTORS = 257;

    private final boolean isVpn;

    public LinkstateNlriParser(final boolean isVpn) {
        this.isVpn = isVpn;
    }

    private static NodeIdentifier parseLink(final CLinkstateDestinationBuilder builder, final ByteBuf buffer, final NodeIdentifier localDescriptors)
            throws BGPParsingException {
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        final NodeIdentifier remote = null;
        NodeIdentifier remoteDescriptors = null;
        if (type == REMOTE_NODE_DESCRIPTORS) {
            remoteDescriptors = NodeNlriParser.parseNodeDescriptors(buffer.readSlice(length));
        }
        builder.setObjectType(new LinkCaseBuilder()
            .setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder().setNodeIdentifier(localDescriptors).build())
            .setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder().setNodeIdentifier(remoteDescriptors).build())
            .setLinkDescriptors(LinkNlriParser.parseLinkDescriptors(buffer.slice())).build());
        return remote;
    }

    /**
     * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
     *
     * @param nlri as byte array
     * @return {@link CLinkstateDestination}
     * @throws BGPParsingException if parsing was unsuccessful
     */
    public static List<CLinkstateDestination> parseNlri(final ByteBuf nlri, final boolean isVpn) throws BGPParsingException {
        final List<CLinkstateDestination> dests = new ArrayList<>();
        while (nlri.isReadable()) {
            final CLinkstateDestinationBuilder builder = new CLinkstateDestinationBuilder();
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
            final int localtype = nlri.readUnsignedShort();
            final int locallength = nlri.readUnsignedShort();
            if (localtype == LOCAL_NODE_DESCRIPTORS) {
                localDescriptor = NodeNlriParser.parseNodeDescriptors(nlri.readSlice(locallength));
            }
            final int restLength = length - (isVpn ? ROUTE_DISTINGUISHER_LENGTH : 0) - PROTOCOL_ID_LENGTH - IDENTIFIER_LENGTH
                - TYPE_LENGTH - LENGTH_SIZE - locallength;
            LOG.trace("Restlength {}", restLength);
            final ByteBuf rest = nlri.readSlice(restLength);
            switch (type) {
            case Link:
                parseLink(builder, rest, localDescriptor);
                break;
            case Ipv4Prefix:
                builder.setObjectType(new PrefixCaseBuilder()
                    .setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder().setNodeIdentifier(localDescriptor).build())
                    .setPrefixDescriptors(PrefixNlriParser.parsePrefixDescriptors(rest, true)).build());
                break;
            case Ipv6Prefix:
                builder.setObjectType(new PrefixCaseBuilder()
                    .setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder().setNodeIdentifier(localDescriptor).build())
                    .setPrefixDescriptors(PrefixNlriParser.parsePrefixDescriptors(rest, false)).build());
                break;
            case Node:
                // node nlri is already parsed as it contains only the common fields for node and link nlri
                builder.setObjectType(new NodeCaseBuilder().setNodeDescriptors(new NodeDescriptorsBuilder().setNodeIdentifier(localDescriptor).build()).build());
                break;
            default:
                break;
            }
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
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
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
        final ByteBuf ldescs = Unpooled.buffer();
        switch (destination.getNlriType()) {
        case Ipv4Prefix:
        case Ipv6Prefix:
            final PrefixCase pCase = (PrefixCase)destination.getObjectType();
            NodeNlriParser.serializeNodeDescriptors(pCase.getAdvertisingNodeDescriptors().getNodeIdentifier(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS, ldescs, nlriByteBuf);
            if (pCase.getPrefixDescriptors() != null) {
                PrefixNlriParser.serializePrefixDescriptors(pCase.getPrefixDescriptors(), nlriByteBuf);
            }
            break;
        case Link:
            final LinkCase lCase = (LinkCase)destination.getObjectType();
            NodeNlriParser.serializeNodeDescriptors(lCase.getLocalNodeDescriptors().getNodeIdentifier(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS, ldescs, nlriByteBuf);
            final ByteBuf rdescs = Unpooled.buffer();
            NodeNlriParser.serializeNodeDescriptors(lCase.getRemoteNodeDescriptors().getNodeIdentifier(), rdescs);
            TlvUtil.writeTLV(REMOTE_NODE_DESCRIPTORS, rdescs, nlriByteBuf);
            if (lCase.getLinkDescriptors() != null) {
                LinkNlriParser.serializeLinkDescriptors(lCase.getLinkDescriptors(), nlriByteBuf);
            }
            break;
        case Node:
            final NodeCase nCase = (NodeCase)destination.getObjectType();
            NodeNlriParser.serializeNodeDescriptors(nCase.getNodeDescriptors().getNodeIdentifier(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS, ldescs, nlriByteBuf);
            break;
        default:
            LOG.warn("Unknown NLRI Type.");
            break;
        }
        TlvUtil.writeTLV(destination.getNlriType().getIntValue(), nlriByteBuf, buffer);
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
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase
                linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) routes.getDestinationType();

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
