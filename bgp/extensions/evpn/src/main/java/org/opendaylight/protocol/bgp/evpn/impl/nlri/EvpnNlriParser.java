/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.parseRouteDistinguisher;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.serializeRouteDistinquisher;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.extractRouteDistinguisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvpnNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(EvpnNlriParser.class);
    private static final NodeIdentifier EVPN_CHOICE_NID = new NodeIdentifier(EvpnChoice.QNAME);
    private static final NodeIdentifier  PATH_ID_NID =
            new NodeIdentifier(QName.create(EvpnChoice.QNAME, "path-id").intern());

    @FunctionalInterface
    private interface ExtractionInterface {
        EvpnChoice check(EvpnRegistry reg, ChoiceNode cont);
    }

    public static EvpnDestination extractEvpnDestination(final DataContainerNode route) {
        return extractDestination(route, EvpnRegistry::serializeEvpnModel);
    }

    private static EvpnDestination extractDestination(final DataContainerNode route,
            final ExtractionInterface extract) {
        final EvpnRegistry reg = SimpleEvpnNlriRegistry.getInstance();
        final ChoiceNode cont = (ChoiceNode) route.findChildByArg(EVPN_CHOICE_NID).get();
        final EvpnChoice evpnValue = extract.check(reg, cont);
        if (evpnValue == null) {
            LOG.warn("Unrecognized Nlri {}", cont);
            return null;
        }
        return new EvpnDestinationBuilder()
                .setRouteDistinguisher(extractRouteDistinguisher(route))
                .setPathId(PathIdUtil.buildPathId(route, PATH_ID_NID))
                .setEvpnChoice(evpnValue).build();
    }

    public static EvpnDestination extractRouteKeyDestination(final DataContainerNode evpnChoice) {
        return extractDestination(evpnChoice, EvpnRegistry::serializeEvpnRouteKey);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<EvpnDestination> dst = parseNlri(nlri, constraint, builder.getAfi(), builder.getSafi());

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes
                    .mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder()
                    .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                        .bgp.evpn.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                            .destination.evpn._case.DestinationEvpnBuilder().setEvpnDestination(dst).build())
                .build())
            .build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<EvpnDestination> dst = parseNlri(nlri, constraint, builder.getAfi(), builder.getSafi());

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationEvpnCaseBuilder().setDestinationEvpn(new DestinationEvpnBuilder()
                    .setEvpnDestination(dst).build()).build()).build());
    }

    private static @Nullable List<EvpnDestination> parseNlri(final ByteBuf nlri,
            final PeerSpecificParserConstraint constraints,
            final AddressFamily afi, final SubsequentAddressFamily safi) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<EvpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final EvpnDestinationBuilder builder = new EvpnDestinationBuilder();
            if (MultiPathSupportUtil.isTableTypeSupported(constraints, new BgpTableTypeImpl(afi, safi))) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final NlriType type = NlriType.forValue(nlri.readUnsignedByte());
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setRouteDistinguisher(parseRouteDistinguisher(nlriBuf));
            builder.setEvpnChoice(SimpleEvpnNlriRegistry.getInstance().parseEvpn(type, nlriBuf));
            dests.add(builder.build());
        }
        return dests;
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AttributesReach pathAttributes1 = pathAttributes.augmentation(AttributesReach.class);
        final AttributesUnreach pathAttributes2 = pathAttributes.augmentation(AttributesUnreach.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof DestinationEvpnCase) {
                final DestinationEvpnCase evpnCase = (DestinationEvpnCase) routes.getDestinationType();
                serializeNlri(evpnCase.getDestinationEvpn().getEvpnDestination(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            final WithdrawnRoutes withdrawnRoutes = mpUnreachNlri.getWithdrawnRoutes();
            if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType()
                    instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120
                    .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCase evpnCase =
                        (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update
                                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                .DestinationEvpnCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(evpnCase.getDestinationEvpn().getEvpnDestination(), byteAggregator);
            }
        }
    }

    public static void serializeNlri(final List<EvpnDestination> destinationList, final ByteBuf output) {
        ByteBuf nlriOutput = null;
        for (final EvpnDestination dest : destinationList) {
            final ByteBuf nlriCommon = Unpooled.buffer();
            serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriCommon);
            nlriOutput = SimpleEvpnNlriRegistry.getInstance().serializeEvpn(dest.getEvpnChoice(), nlriCommon);
        }
        output.writeBytes(nlriOutput);
    }

    @Override
    public boolean convertMpReachToMpUnReach(final MpReachNlri mpReachNlri, final MpUnreachNlriBuilder builder) {
        final WithdrawnRoutesBuilder withdrawnRoutes = new WithdrawnRoutesBuilder(builder.getWithdrawnRoutes());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp
                .unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder destinationType =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder(
                                (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120
                                        .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                        .DestinationEvpnCase) withdrawnRoutes.getDestinationType());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp
                .unreach.nlri.withdrawn.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder
                destinationEvpn = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.evpn.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.evpn._case.DestinationEvpnBuilder(destinationType.getDestinationEvpn());
        List<EvpnDestination> evpnDestination = destinationEvpn.getEvpnDestination();
        if (evpnDestination == null) {
            evpnDestination = new ArrayList<>();
        }
        evpnDestination.addAll(((DestinationEvpnCase) mpReachNlri.getAdvertizedRoutes().getDestinationType())
                .getDestinationEvpn().getEvpnDestination());
        builder.setWithdrawnRoutes(withdrawnRoutes
                .setDestinationType(destinationType
                        .setDestinationEvpn(destinationEvpn
                                .setEvpnDestination(evpnDestination)
                                .build())
                        .build())
                .build());
        return true;
    }
}
