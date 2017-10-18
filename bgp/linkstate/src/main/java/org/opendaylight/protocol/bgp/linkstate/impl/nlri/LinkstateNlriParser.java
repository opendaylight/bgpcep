/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractTeLspNlriCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Identifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
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
    public static final NodeIdentifier PROTOCOL_ID_NID = new NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "protocol-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier IDENTIFIER_NID = new NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "identifier").intern());
    @VisibleForTesting
    private static final NodeIdentifier DISTINGUISHER_NID = new NodeIdentifier(QName.create(CLinkstateDestination.QNAME, "distinguisher").intern());
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
            final CLinkstateDestination destination = this.nlriTypeReg.parseNlriType(nlri);
            if (destination == null) {
                continue;
            }
            dests.add(destination);
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
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().setCLinkstateDestination(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<CLinkstateDestination> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                new DestinationLinkstateBuilder().setCLinkstateDestination(dst).build()).build()).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            serializeAdvertisedRoutes((pathAttributes1.getMpReachNlri()).getAdvertizedRoutes(), byteAggregator);
        } else if (pathAttributes2 != null) {
            serializeWithDrawnRoutes(pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes(), byteAggregator);
        }
    }

    private void serializeWithDrawnRoutes(final WithdrawnRoutes withdrawnRoutes, final ByteBuf byteAggregator) {
        if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType() instanceof DestinationLinkstateCase) {
            final DestinationLinkstateCase linkstateCase = (DestinationLinkstateCase) withdrawnRoutes.getDestinationType();
            for (final CLinkstateDestination cLinkstateDestination : linkstateCase.getDestinationLinkstate().getCLinkstateDestination()) {
                this.nlriTypeReg.serializeNlriType(cLinkstateDestination, byteAggregator);
            }
        }
    }

    private void serializeAdvertisedRoutes(final AdvertizedRoutes advertizedRoutes, final ByteBuf byteAggregator) {
        if (advertizedRoutes != null && advertizedRoutes.getDestinationType()
            instanceof
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase
                linkstateCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase) advertizedRoutes.getDestinationType();

            for (final CLinkstateDestination cLinkstateDestination : linkstateCase.getDestinationLinkstate().getCLinkstateDestination()) {
                this.nlriTypeReg.serializeNlriType(cLinkstateDestination, byteAggregator);
            }
        }
    }

    // FIXME : use codec
    private static int domProtocolIdValue(final String protocolId) {
        switch (protocolId) {
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
        case "ospf-v3":
            return ProtocolId.OspfV3.getIntValue();
        case "rsvp-te":
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
        serializeCommonParts(builder, linkstate);

        final ChoiceNode objectType = (ChoiceNode) linkstate.getChild(OBJECT_TYPE_NID).get();
        if (objectType.getChild(ADVERTISING_NODE_DESCRIPTORS_NID).isPresent()) {
            serializeAdvertisedNodeDescriptor(builder, objectType);
        } else if (objectType.getChild(LOCAL_NODE_DESCRIPTORS_NID).isPresent()) {
            serializeLocalNodeDescriptor(builder, objectType);
        } else if (objectType.getChild(NODE_DESCRIPTORS_NID).isPresent()) {
            serializeNodeDescriptor(builder, objectType);
        } else if (AbstractTeLspNlriCodec.isTeLsp(objectType)) {
            builder.setObjectType(AbstractTeLspNlriCodec.serializeTeLsp(objectType));
        } else {
            LOG.warn("Unknown Object Type: {}.", objectType);
        }
        return builder.build();
    }

    private static void serializeNodeDescriptor(final CLinkstateDestinationBuilder builder, final ChoiceNode objectType) {
        final NodeCaseBuilder nodeBuilder = new NodeCaseBuilder();
        // node descriptors
        nodeBuilder.setNodeDescriptors(NodeNlriParser.serializeNodeDescriptors((ContainerNode) objectType.getChild(NODE_DESCRIPTORS_NID).get()));
        builder.setObjectType(nodeBuilder.build());
    }

    private static void serializeLocalNodeDescriptor(final CLinkstateDestinationBuilder builder, final ChoiceNode objectType) {
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
    }

    private static void serializeAdvertisedNodeDescriptor(final CLinkstateDestinationBuilder builder, final ChoiceNode objectType) {
        // prefix node descriptors
        final PrefixCaseBuilder prefixBuilder = new PrefixCaseBuilder();
        prefixBuilder.setAdvertisingNodeDescriptors(NodeNlriParser.serializeAdvNodeDescriptors((ContainerNode) objectType.getChild(
            ADVERTISING_NODE_DESCRIPTORS_NID).get()));

        // prefix descriptors
        final Optional<DataContainerChild<? extends PathArgument, ?>> prefixDescriptors = objectType.getChild(PREFIX_DESCRIPTORS_NID);
        if (prefixDescriptors.isPresent()) {
            prefixBuilder.setPrefixDescriptors(AbstractPrefixNlriParser.serializePrefixDescriptors((ContainerNode) prefixDescriptors.get()));
        }
        builder.setObjectType(prefixBuilder.build());
    }

    private static void serializeCommonParts(final CLinkstateDestinationBuilder builder, final DataContainerNode<? extends PathArgument> linkstate) {
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
    }
}
