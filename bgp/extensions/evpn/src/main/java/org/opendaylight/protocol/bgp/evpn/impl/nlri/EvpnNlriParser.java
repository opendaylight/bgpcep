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
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistry;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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

    public static EvpnDestination extractEvpnDestination(final DataContainerNode<? extends PathArgument> route) {
        return extractDestination(route, EvpnRegistry::serializeEvpnModel);
    }

    private static EvpnDestination extractDestination(final DataContainerNode<? extends PathArgument> route,
            final ExtractionInterface extract) {
        final EvpnRegistry reg = SimpleEvpnNlriRegistry.getInstance();
        final ChoiceNode cont = (ChoiceNode) route.getChild(EVPN_CHOICE_NID).get();
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

    public static EvpnDestination extractRouteKeyDestination(
            final DataContainerNode<? extends PathArgument> evpnChoice) {
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
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update.attributes
                    .mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder()
                    .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn
                            .rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination
                            .evpn._case.DestinationEvpnBuilder().setEvpnDestination(dst).build()).build()).build());
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

    @Nullable
    private static List<EvpnDestination> parseNlri(final ByteBuf nlri, final PeerSpecificParserConstraint constraints,
            final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
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
        final Attributes1 pathAttributes1 = pathAttributes.augmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.augmentation(Attributes2.class);
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
                    instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329
                    .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCase evpnCase =
                        (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.update
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
}
