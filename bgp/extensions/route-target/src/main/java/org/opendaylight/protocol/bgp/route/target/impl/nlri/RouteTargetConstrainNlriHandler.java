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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.constrain.advertized._case.DestinationRouteTargetConstrainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCaseBuilder;

/**
 * Route Target Nlri Handler.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetConstrainNlriHandler implements NlriParser, NlriSerializer {

    private static DestinationRouteTargetConstrainAdvertizedCase parseIpv4ReachNlri(
            final ByteBuf nlri,
            final boolean addPathSupported) {
        final List<RouteTargetConstrainDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final RouteTargetConstrainDestinationBuilder builder = new RouteTargetConstrainDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final Integer type = (int) nlri.readUnsignedByte();
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setRouteTargetConstrainChoice(SimpleRouteTargetConstrainNlriRegistry.getInstance()
                    .parseRouteTargetConstrain(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationRouteTargetConstrainAdvertizedCaseBuilder().setDestinationRouteTargetConstrain(
                new DestinationRouteTargetConstrainBuilder().setRouteTargetConstrainDestination(dests).build()).build();
    }

    private static DestinationRouteTargetConstrainWithdrawnCase parseIpv4UnreachNlri(
            final ByteBuf nlri,
            final boolean addPathSupported) {
        final List<RouteTargetConstrainDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final RouteTargetConstrainDestinationBuilder builder = new RouteTargetConstrainDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final Integer type = (int) nlri.readUnsignedByte();
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setRouteTargetConstrainChoice(SimpleRouteTargetConstrainNlriRegistry.getInstance()
                    .parseRouteTargetConstrain(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationRouteTargetConstrainWithdrawnCaseBuilder().setDestinationRouteTargetConstrain(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.route.target.constrain
                        .withdrawn._case.DestinationRouteTargetConstrainBuilder()
                        .setRouteTargetConstrainDestination(dests).build()).build();
    }

    public static void serializeNlri(final List<RouteTargetConstrainDestination> destinationList, final ByteBuf output) {
        for (final RouteTargetConstrainDestination dest : destinationList) {
            output.writeBytes(SimpleRouteTargetConstrainNlriRegistry.getInstance()
                    .serializeRouteTargetConstrain(dest.getRouteTargetConstrainChoice()));
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
            if (routes != null && routes.getDestinationType() instanceof DestinationRouteTargetConstrainAdvertizedCase) {
                final DestinationRouteTargetConstrainAdvertizedCase reach
                        = (DestinationRouteTargetConstrainAdvertizedCase) routes.getDestinationType();
                serializeNlri(reach.getDestinationRouteTargetConstrain().getRouteTargetConstrainDestination(),
                        byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null
                    && withdrawnRoutes.getDestinationType() instanceof DestinationRouteTargetConstrainWithdrawnCase) {
                final DestinationRouteTargetConstrainWithdrawnCase reach
                        = (DestinationRouteTargetConstrainWithdrawnCase) withdrawnRoutes.getDestinationType();
                serializeNlri(reach.getDestinationRouteTargetConstrain().getRouteTargetConstrainDestination(),
                        byteAggregator);
            }
        }
    }
}
