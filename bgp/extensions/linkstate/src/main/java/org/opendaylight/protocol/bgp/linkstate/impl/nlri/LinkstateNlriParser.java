/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractTeLspNlriCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser and serializer for Linkstate NLRI.
 */
public final class LinkstateNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateNlriParser.class);

    @VisibleForTesting
    public static final NodeIdentifier OBJECT_TYPE_NID = NodeIdentifier.create(ObjectType.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier NODE_DESCRIPTORS_NID = NodeIdentifier.create(NodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LOCAL_NODE_DESCRIPTORS_NID = NodeIdentifier.create(LocalNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier REMOTE_NODE_DESCRIPTORS_NID = NodeIdentifier.create(RemoteNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ADVERTISING_NODE_DESCRIPTORS_NID = NodeIdentifier.create(
        AdvertisingNodeDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier PREFIX_DESCRIPTORS_NID = NodeIdentifier.create(PrefixDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier LINK_DESCRIPTORS_NID = NodeIdentifier.create(LinkDescriptors.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier PROTOCOL_ID_NID = NodeIdentifier.create(
            QName.create(CLinkstateDestination.QNAME.getModule(), "protocol-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier IDENTIFIER_NID = NodeIdentifier.create(
            QName.create(CLinkstateDestination.QNAME.getModule(), "identifier").intern());
    @VisibleForTesting
    private static final NodeIdentifier DISTINGUISHER_NID = NodeIdentifier.create(
            QName.create(CLinkstateDestination.QNAME.getModule(), "route-distinguisher").intern());
    private final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();


    /**
     * Parses common parts for Link State Nodes, Links and Prefixes, that includes protocol ID and identifier tlv.
     *
     * @param nlri as byte array
     * @return {@link CLinkstateDestination}
     */
    private List<CLinkstateDestination> parseNlri(final ByteBuf nlri) {
        final List<CLinkstateDestination> dests = new ArrayList<>();
        while (nlri.isReadable()) {
            final CLinkstateDestination destination = nlriTypeReg.parseNlriType(nlri);
            if (destination == null) {
                continue;
            }
            dests.add(destination);
        }
        return dests;
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder()
                        .setDestinationLinkstate(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                                .bgp.linkstate.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination
                                .type.destination.linkstate._case.DestinationLinkstateBuilder()
                                .setCLinkstateDestination(dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build()).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AttributesReach pathAttributes1 = pathAttributes.augmentation(AttributesReach.class);
        final AttributesUnreach pathAttributes2 = pathAttributes.augmentation(AttributesUnreach.class);
        if (pathAttributes1 != null) {
            serializeAdvertisedRoutes(pathAttributes1.getMpReachNlri().getAdvertizedRoutes(), byteAggregator);
        } else if (pathAttributes2 != null) {
            serializeWithDrawnRoutes(pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes(), byteAggregator);
        }
    }

    private void serializeWithDrawnRoutes(final WithdrawnRoutes withdrawnRoutes, final ByteBuf byteAggregator) {
        if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType() instanceof DestinationLinkstateCase) {
            final DestinationLinkstateCase linkstateCase
                    = (DestinationLinkstateCase) withdrawnRoutes.getDestinationType();
            for (final CLinkstateDestination linkstateDestinationCase : linkstateCase.getDestinationLinkstate()
                    .getCLinkstateDestination()) {
                nlriTypeReg.serializeNlriType(linkstateDestinationCase, byteAggregator);
            }
        }
    }

    private void serializeAdvertisedRoutes(final AdvertizedRoutes advertizedRoutes, final ByteBuf byteAggregator) {
        if (advertizedRoutes != null && advertizedRoutes.getDestinationType()
                instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update
                        .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.update
                    .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase
                    linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                        .bgp.linkstate.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                        .DestinationLinkstateCase) advertizedRoutes.getDestinationType();

            for (var linkstateDestinationCase : linkstateCase.getDestinationLinkstate().getCLinkstateDestination()) {
                nlriTypeReg.serializeNlriType(linkstateDestinationCase, byteAggregator);
            }
        }
    }

    // FIXME : use codec
    private static int domProtocolIdValue(final String protocolId) {
        return switch (protocolId) {
            case "isis-level1" -> ProtocolId.IsisLevel1.getIntValue();
            case "isis-level2" -> ProtocolId.IsisLevel2.getIntValue();
            case "ospf" -> ProtocolId.Ospf.getIntValue();
            case "direct" -> ProtocolId.Direct.getIntValue();
            case "static" -> ProtocolId.Static.getIntValue();
            case "ospf-v3" -> ProtocolId.OspfV3.getIntValue();
            case "rsvp-te" -> ProtocolId.RsvpTe.getIntValue();
            case "bgp-epe" -> ProtocolId.BgpEpe.getIntValue();
            case "segment-routing" -> ProtocolId.SegmentRouting.getIntValue();
            default -> 0;
        };
    }

    public static CLinkstateDestination extractLinkstateDestination(final DataContainerNode linkstate) {
        final var builder = new CLinkstateDestinationBuilder()
            .setRouteDistinguisher(RouteDistinguisherUtil.extractRouteDistinguisher(linkstate, DISTINGUISHER_NID));
        final var protocolId = linkstate.childByArg(PROTOCOL_ID_NID);
        if (protocolId != null) {
            // DOM representation contains values as are in the model, not as are in generated enum
            builder.setProtocolId(ProtocolId.forValue(domProtocolIdValue((String) protocolId.body())));
        }
        final var identifier = linkstate.childByArg(IDENTIFIER_NID);
        if (identifier != null) {
            builder.setIdentifier(new Identifier((Uint64) identifier.body()));
        }

        return builder
            .setObjectType(serializeObjectType((ChoiceNode) linkstate.getChildByArg(OBJECT_TYPE_NID)))
            .build();
    }

    private static ObjectType serializeObjectType(final ChoiceNode objectType) {
        final var advNode = objectType.childByArg(ADVERTISING_NODE_DESCRIPTORS_NID);
        if (advNode != null) {
            // prefix node descriptors
            final var builder = new PrefixCaseBuilder()
                .setAdvertisingNodeDescriptors(NodeNlriParser.serializeAdvNodeDescriptors((ContainerNode) advNode));

            // prefix descriptors
            final var prefix = objectType.childByArg(PREFIX_DESCRIPTORS_NID);
            if (prefix != null) {
                builder.setPrefixDescriptors(
                    AbstractPrefixNlriParser.serializePrefixDescriptors((ContainerNode) prefix));
            }
            return builder.build();
        }

        final var localNode = objectType.childByArg(LOCAL_NODE_DESCRIPTORS_NID);
        if (localNode != null) {
            // link local node descriptors
            final var builder = new LinkCaseBuilder()
                .setLocalNodeDescriptors(NodeNlriParser.serializeLocalNodeDescriptors((ContainerNode) localNode));
            // link remote node descriptors
            final var remoteNode = objectType.childByArg(REMOTE_NODE_DESCRIPTORS_NID);
            if (remoteNode != null) {
                builder.setRemoteNodeDescriptors(
                    NodeNlriParser.serializeRemoteNodeDescriptors((ContainerNode) remoteNode));
            }
            // link descriptors
            final var link = objectType.childByArg(LINK_DESCRIPTORS_NID);
            if (link != null) {
                builder.setLinkDescriptors(LinkNlriParser.serializeLinkDescriptors((ContainerNode) link));
            }
            return builder.build();
        }

        final var node = objectType.childByArg(NODE_DESCRIPTORS_NID);
        if (node != null) {
            // node descriptors
            return new NodeCaseBuilder()
                .setNodeDescriptors(NodeNlriParser.serializeNodeDescriptors((ContainerNode) node))
                .build();
        }

        final var teLsp = AbstractTeLspNlriCodec.serializeObjectType(objectType);
        if (teLsp != null) {
            return teLsp;
        }

        LOG.warn("Ignoring unknown Object Type: {}.", objectType);
        return null;
    }
}
