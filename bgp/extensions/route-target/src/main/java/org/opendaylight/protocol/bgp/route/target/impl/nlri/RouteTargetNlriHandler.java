/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl.nlri;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.destination.RouteTargetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetAdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.advertized._case.DestinationRouteTargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetWithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetWithdrawnCaseBuilder;

/**
 * Route Target Nlri Handler.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetNlriHandler implements NlriParser, NlriSerializer {

    static DestinationRouteTargetAdvertizedCase parseIpv4ReachNlri(
            final ByteBuf nlri,
            final boolean addPathSupported) {
        final List<RouteTargetDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final RouteTargetDestinationBuilder builder = new RouteTargetDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final Integer type = (int) nlri.readUnsignedByte();
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setRouteTargetChoice(SimpleRouteTargetNlriRegistry.getInstance().parseRouteTarget(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationRouteTargetAdvertizedCaseBuilder().setDestinationRouteTarget(
                new DestinationRouteTargetBuilder().setRouteTargetDestination(dests).build()).build();
    }

    static DestinationRouteTargetWithdrawnCase parseIpv4UnreachNlri(
            final ByteBuf nlri,
            final boolean addPathSupported) {
        final List<RouteTargetDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final RouteTargetDestinationBuilder builder = new RouteTargetDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final Integer type = (int) nlri.readUnsignedByte();
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setRouteTargetChoice(SimpleRouteTargetNlriRegistry.getInstance().parseRouteTarget(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationRouteTargetWithdrawnCaseBuilder().setDestinationRouteTarget(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.route.target
                        .withdrawn._case.DestinationRouteTargetBuilder().setRouteTargetDestination(dests).build())
                .build();
    }

    public static void serializeNlri(final List<RouteTargetDestination> destinationList, final ByteBuf output) {
        for (final RouteTargetDestination dest : destinationList) {
            output.writeBytes(SimpleRouteTargetNlriRegistry.getInstance().serializeRouteTarget(dest.getRouteTargetChoice()));
        }
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                .setDestinationType(parseIpv4ReachNlri(nlri, mPathSupported)).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                .setDestinationType(parseIpv4UnreachNlri(nlri, mPathSupported)).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final Attributes1 pathAttributes1 = pathAttributes.augmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.augmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof DestinationRouteTargetAdvertizedCase) {
                final DestinationRouteTargetAdvertizedCase reach
                        = (DestinationRouteTargetAdvertizedCase) routes.getDestinationType();
                serializeNlri(reach.getDestinationRouteTarget().getRouteTargetDestination(),
                        byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null
                    && withdrawnRoutes.getDestinationType() instanceof DestinationRouteTargetWithdrawnCase) {
                final DestinationRouteTargetWithdrawnCase reach
                        = (DestinationRouteTargetWithdrawnCase) withdrawnRoutes.getDestinationType();
                serializeNlri(reach.getDestinationRouteTarget().getRouteTargetDestination(),
                        byteAggregator);
            }
        }
    }
}
