/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
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

    private static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;
    private static final int REMOTE_NODE_DESCRIPTORS_TYPE = 257;

    @VisibleForTesting
    public static final NodeIdentifier OBJECT_TYPE_NID = new NodeIdentifier(ObjectType.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier NODE_DESCRIPTORS_NID = new NodeIdentifier(NodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LOCAL_NODE_DESCRIPTORS_NID = new NodeIdentifier(LocalNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier REMOTE_NODE_DESCRIPTORS_NID = new NodeIdentifier(RemoteNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ADVERTISING_NODE_DESCRIPTORS_NID = new NodeIdentifier(AdvertisingNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier PREFIX_DESCRIPTORS_NID = new NodeIdentifier(PrefixDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LINK_DESCRIPTORS_NID = new NodeIdentifier(LinkDescriptors.QNAME);

    @VisibleForTesting
    public static final NodeIdentifier TE_LSP_NID = new NodeIdentifier(TeLspCase.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier DISTINGUISHER_NID = new NodeIdentifier(QName.cachedReference(QName.create(CLinkstateDestination.QNAME, "route-distinguisher")));
    @VisibleForTesting
    public static final NodeIdentifier PROTOCOL_ID_NID = new NodeIdentifier(QName.cachedReference(QName.create(CLinkstateDestination.QNAME, "protocol-id")));
    @VisibleForTesting
    public static final NodeIdentifier IDENTIFIER_NID = new NodeIdentifier(QName.cachedReference(QName.create(CLinkstateDestination.QNAME, "identifier")));

    private final boolean isVpn;

    public LinkstateNlriParser(final boolean isVpn) {
        this.isVpn = isVpn;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier parseLink(final CLinkstateDestinationBuilder builder, final ByteBuf buffer, final LocalNodeDescriptors localDescriptors)
            throws BGPParsingException {
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier remote = null;
        RemoteNodeDescriptors remoteDescriptors = null;
        if (type == REMOTE_NODE_DESCRIPTORS_TYPE) {
            remoteDescriptors = (RemoteNodeDescriptors) NodeNlriParser.parseNodeDescriptors(buffer.readSlice(length), NlriType.Link, false);
        }
        builder.setObjectType(new LinkCaseBuilder()
            .setLocalNodeDescriptors(localDescriptors)
            .setRemoteNodeDescriptors(remoteDescriptors)
            .setLinkDescriptors(LinkNlriParser.parseLinkDescriptors(buffer.slice())).build());
        return remote;
    }

    /**
     * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
     *
     * @param nlri as byte array
     * @param isVpn flag which determines that destination has route distinguisher
     * @return {@link CLinkstateDestination}
     * @throws BGPParsingException if parsing was unsuccessful
     */
    public static List<CLinkstateDestination> parseNlri(final ByteBuf nlri, final boolean isVpn) throws BGPParsingException {
        final List<CLinkstateDestination> dests = new ArrayList<>();
        while (nlri.isReadable()) {
            final CLinkstateDestinationBuilder builder = new CLinkstateDestinationBuilder();
            final NlriType type = NlriType.forValue(nlri.readUnsignedShort());

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
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor = null;
            ByteBuf rest = null;
            if(!type.equals(NlriType.Ipv4TeLsp) && !type.equals(NlriType.Ipv6TeLsp)) {
                final int localtype = nlri.readUnsignedShort();
                final int locallength = nlri.readUnsignedShort();
                if (localtype == LOCAL_NODE_DESCRIPTORS_TYPE) {
                    localDescriptor = NodeNlriParser.parseNodeDescriptors(nlri.readSlice(locallength), type, true);
                }
                final int restLength = length - (isVpn ? ROUTE_DISTINGUISHER_LENGTH : 0) - PROTOCOL_ID_LENGTH -
                    IDENTIFIER_LENGTH - TYPE_LENGTH - LENGTH_SIZE - locallength;
                LOG.trace("Restlength {}", restLength);
                rest = nlri.readSlice(restLength);
            }

            switch (type) {
            case Link:
                parseLink(builder, rest, (LocalNodeDescriptors) localDescriptor);
                break;
            case Ipv4Prefix:
                builder.setObjectType(new PrefixCaseBuilder()
                    .setAdvertisingNodeDescriptors((AdvertisingNodeDescriptors) localDescriptor)
                    .setPrefixDescriptors(PrefixNlriParser.parsePrefixDescriptors(rest, true)).build());
                break;
            case Ipv6Prefix:
                builder.setObjectType(new PrefixCaseBuilder()
                    .setAdvertisingNodeDescriptors((AdvertisingNodeDescriptors) localDescriptor)
                    .setPrefixDescriptors(PrefixNlriParser.parsePrefixDescriptors(rest, false)).build());
                break;
            case Node:
                // node nlri is already parsed as it contains only the common fields for node and link nlri
                builder.setObjectType(new NodeCaseBuilder().setNodeDescriptors((NodeDescriptors) localDescriptor).build());
                break;
            case Ipv4TeLsp:
                builder.setObjectType(parseIpv4TeLsp(nlri));
                break;
            case Ipv6TeLsp:
                builder.setObjectType(parseIpv6TeLsp(nlri));
                break;
            default:
                break;
            }
            dests.add(builder.build());
        }
        return dests;
    }

    private static TeLspCase parseIpv6TeLsp(final ByteBuf nlri) {
        final Ipv6CaseBuilder ipv6Builder = new Ipv6CaseBuilder();
        ipv6Builder.setIpv6TunnelSenderAddress(Ipv6Util.addressForByteBuf(nlri));
        final TunnelId tunnelId6 = new TunnelId(nlri.readUnsignedShort());
        final LspId lspId6 = new LspId((long)nlri.readUnsignedShort());
        ipv6Builder.setIpv6TunnelEndpointAddress(Ipv6Util.addressForByteBuf(nlri));
        return new TeLspCaseBuilder().setAddressFamily(ipv6Builder.build()).setLspId(lspId6).setTunnelId(tunnelId6).build();
    }

    private static TeLspCase parseIpv4TeLsp(final ByteBuf nlri) {
        final Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(nlri));
        final TunnelId tunnelId = new TunnelId(nlri.readUnsignedShort());
        final LspId lspId = new LspId((long)nlri.readUnsignedShort());
        ipv4Builder.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(nlri));
        return new TeLspCaseBuilder().setAddressFamily(ipv4Builder.build()).setLspId(lspId).setTunnelId(tunnelId).build();
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri, this.isVpn);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
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
     * @param buffer where Linkstate NLRI will be serialized
     */
    public static void serializeNlri(final CLinkstateDestination destination, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        if (destination.getDistinguisher() != null) {
            nlriByteBuf.writeBytes(destination.getDistinguisher().getValue().toByteArray());
        }
        nlriByteBuf.writeByte(destination.getProtocolId().getIntValue());
        nlriByteBuf.writeLong(destination.getIdentifier().getValue().longValue());
        final ByteBuf ldescs = Unpooled.buffer();
        final ObjectType ot = destination.getObjectType();
        NlriType nlriType = null;
        if (ot instanceof PrefixCase) {
            final PrefixCase pCase = (PrefixCase) destination.getObjectType();
            NodeNlriParser.serializeNodeIdentifier(pCase.getAdvertisingNodeDescriptors(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, ldescs, nlriByteBuf);
            if (pCase.getPrefixDescriptors() != null) {
                PrefixNlriParser.serializePrefixDescriptors(pCase.getPrefixDescriptors(), nlriByteBuf);
                if (pCase.getPrefixDescriptors().getIpReachabilityInformation().getIpv4Prefix() != null) {
                    nlriType = NlriType.Ipv4Prefix;
                } else {
                    nlriType = NlriType.Ipv6Prefix;
                }
            }
        } else if (ot instanceof LinkCase) {
            final LinkCase lCase = (LinkCase) destination.getObjectType();
            NodeNlriParser.serializeNodeIdentifier(lCase.getLocalNodeDescriptors(), ldescs);
            NodeNlriParser.serializeEpeNodeDescriptors(lCase.getLocalNodeDescriptors(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, ldescs, nlriByteBuf);
            final ByteBuf rdescs = Unpooled.buffer();
            NodeNlriParser.serializeNodeIdentifier(lCase.getRemoteNodeDescriptors(), rdescs);
            NodeNlriParser.serializeEpeNodeDescriptors(lCase.getRemoteNodeDescriptors(), rdescs);
            TlvUtil.writeTLV(REMOTE_NODE_DESCRIPTORS_TYPE, rdescs, nlriByteBuf);
            if (lCase.getLinkDescriptors() != null) {
                LinkNlriParser.serializeLinkDescriptors(lCase.getLinkDescriptors(), nlriByteBuf);
            }
            nlriType = NlriType.Link;
        } else if (ot instanceof NodeCase) {
            final NodeCase nCase = (NodeCase) destination.getObjectType();
            NodeNlriParser.serializeNodeIdentifier(nCase.getNodeDescriptors(), ldescs);
            TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, ldescs, nlriByteBuf);
            nlriType = NlriType.Node;
        } else if (ot instanceof TeLspCase) {
            final TeLspCase teLSP = (TeLspCase) destination.getObjectType();
            final AddressFamily afi = teLSP.getAddressFamily();
            nlriType = TeLspNlriParser.serializeIpvTSA(afi, ldescs);
            TeLspNlriParser.serializeTunnelID(teLSP.getTunnelId(), ldescs);
            TeLspNlriParser.serializeLspID(teLSP.getLspId(), ldescs);
            TeLspNlriParser.serializeTEA(afi, ldescs);
        } else {
            LOG.warn("Unknown NLRI Type.");
        }
        TlvUtil.writeTLV(nlriType.getIntValue(), nlriByteBuf, buffer);
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null &&
                routes.getDestinationType()
                instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase
                    linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) routes.getDestinationType();

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

    // FIXME : use codec
    private static int domProtocolIdValue(final String protocolId) {
        switch (protocolId) {
        case "unknown":
            return ProtocolId.Unknown.getIntValue();
        case "isis-level1":
            return ProtocolId.IsisLevel1.getIntValue();
        case "isis-level2":
            return ProtocolId.IsisLevel2.getIntValue();
        case "ospf":
            return ProtocolId.Ospf.getIntValue();
        case "direct":
            return ProtocolId.Direct.getIntValue();
        case "static":
            return ProtocolId.Static.getIntValue();
        case "rsvpte":
            return ProtocolId.RsvpTe.getIntValue();
        case "bgp-epe":
            return ProtocolId.BgpEpe.getIntValue();
        case "segment-routing":
            return ProtocolId.SegmentRouting.getIntValue();
        default:
            return 0;
        }
    }

    public static CLinkstateDestination extractLinkstateDestination(final DataContainerNode<? extends PathArgument> linkstate) {
        final CLinkstateDestinationBuilder builder = new CLinkstateDestinationBuilder();

        // serialize common parts
        final Optional<DataContainerChild<? extends PathArgument, ?>> distinguisher = linkstate.getChild(DISTINGUISHER_NID);
        if (distinguisher.isPresent()) {
            builder.setDistinguisher(new RouteDistinguisher((BigInteger) distinguisher.get().getValue()));
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> protocolId = linkstate.getChild(PROTOCOL_ID_NID);
        // DOM representation contains values as are in the model, not as are in generated enum
        if (protocolId.isPresent()) {
            builder.setProtocolId(ProtocolId.forValue(domProtocolIdValue((String) protocolId.get().getValue())));
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> identifier = linkstate.getChild(IDENTIFIER_NID);
        if (identifier.isPresent()) {
            builder.setIdentifier(new Identifier((BigInteger) identifier.get().getValue()));
        }

        final ChoiceNode objectType = (ChoiceNode) linkstate.getChild(OBJECT_TYPE_NID).get();
        if (!builder.getProtocolId().equals(ProtocolId.RsvpTe)) {
            if (objectType.getChild(ADVERTISING_NODE_DESCRIPTORS_NID).isPresent()) {
                // prefix node descriptors
                final PrefixCaseBuilder prefixBuilder = new PrefixCaseBuilder();
                prefixBuilder.setAdvertisingNodeDescriptors(NodeNlriParser.serializeAdvNodeDescriptors((ContainerNode) objectType.getChild(
                    ADVERTISING_NODE_DESCRIPTORS_NID).get()));

                // prefix descriptors
                final Optional<DataContainerChild<? extends PathArgument, ?>> prefixDescriptors = objectType.getChild(PREFIX_DESCRIPTORS_NID);
                if (prefixDescriptors.isPresent()) {
                    prefixBuilder.setPrefixDescriptors(PrefixNlriParser.serializePrefixDescriptors((ContainerNode) prefixDescriptors.get()));
                }
                builder.setObjectType(prefixBuilder.build());
            } else if (objectType.getChild(LOCAL_NODE_DESCRIPTORS_NID).isPresent()) {
                // link local node descriptors
                final LinkCaseBuilder linkBuilder = new LinkCaseBuilder();

                linkBuilder.setLocalNodeDescriptors(NodeNlriParser.serializeLocalNodeDescriptors((ContainerNode) objectType.getChild(LOCAL_NODE_DESCRIPTORS_NID).get()));
                // link remote node descriptors
                if (objectType.getChild(REMOTE_NODE_DESCRIPTORS_NID).isPresent()) {
                    linkBuilder.setRemoteNodeDescriptors(NodeNlriParser.serializeRemoteNodeDescriptors((ContainerNode) objectType.getChild(REMOTE_NODE_DESCRIPTORS_NID).get()));
                }
                // link descriptors
                final Optional<DataContainerChild<? extends PathArgument, ?>> linkDescriptors = objectType.getChild(LINK_DESCRIPTORS_NID);
                if (linkDescriptors.isPresent()) {
                    linkBuilder.setLinkDescriptors(LinkNlriParser.serializeLinkDescriptors((ContainerNode) linkDescriptors.get()));
                }
                builder.setObjectType(linkBuilder.build());
            } else if (objectType.getChild(NODE_DESCRIPTORS_NID).isPresent()) {
                final NodeCaseBuilder nodeBuilder = new NodeCaseBuilder();
                // node descriptors
                nodeBuilder.setNodeDescriptors(NodeNlriParser.serializeNodeDescriptors((ContainerNode) objectType.getChild(NODE_DESCRIPTORS_NID).get()));
                builder.setObjectType(nodeBuilder.build());
            } else {
                LOG.warn("Unknown Object Type.");
            }
        } else if (objectType.getChild(TE_LSP_NID).isPresent()) {
            builder.setObjectType(TeLspNlriParser.serializeTeLsp((ContainerNode) objectType.getChild(TE_LSP_NID).get()));
        }
        return builder.build();
    }
}
