/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.constrain.advertized._case.DestinationRouteTargetConstrainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCaseBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Route Target Nlri Handler.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetConstrainNlriHandler implements NlriParser, NlriSerializer {
    private static final short RT_BITS_LENGTH = 96;

    private static List<RouteTargetConstrainDestination> parseNlriDestinations(final ByteBuf nlri,
            final boolean addPathSupported) {
        final List<RouteTargetConstrainDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final RouteTargetConstrainDestinationBuilder builder = new RouteTargetConstrainDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final int length = nlri.readUnsignedByte() / 8;
            final ByteBuf nlriBuf = nlri.readSlice(length);
            Integer type = null;
            if (length != 0) {
                builder.setOriginAs(new AsNumber(ByteBufUtils.readUint32(nlriBuf)));
                type = (int) nlriBuf.readUnsignedByte();
                //Skip Subtype
                nlriBuf.skipBytes(1);
            }
            builder.setRouteTargetConstrainChoice(SimpleRouteTargetConstrainNlriRegistry.getInstance()
                    .parseRouteTargetConstrain(type, nlriBuf));
            dests.add(builder.build());
        }
        return dests;
    }

    public static ByteBuf serializeNlriDestinations(final List<RouteTargetConstrainDestination> destinationList) {
        final ByteBuf nlri = Unpooled.buffer();
        for (final RouteTargetConstrainDestination dest : destinationList) {
            final RouteTargetConstrainChoice rtcChoice = dest.getRouteTargetConstrainChoice();
            if (rtcChoice instanceof RouteTargetConstrainDefaultCase) {
                nlri.writeByte(0);
            } else {
                nlri.writeByte(RT_BITS_LENGTH);
                final AsNumber originAs = dest.getOriginAs();
                if (originAs != null) {
                    writeUnsignedInt(originAs.getValue(), nlri);
                }
                nlri.writeBytes(SimpleRouteTargetConstrainNlriRegistry.getInstance()
                        .serializeRouteTargetConstrain(rtcChoice));
            }
        }
        return nlri;
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
                .setDestinationType(new DestinationRouteTargetConstrainAdvertizedCaseBuilder()
                        .setDestinationRouteTargetConstrain(new DestinationRouteTargetConstrainBuilder()
                                .setRouteTargetConstrainDestination(parseNlriDestinations(nlri, mPathSupported))
                                .build()).build()).build());
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
                .setDestinationType(new DestinationRouteTargetConstrainWithdrawnCaseBuilder()
                        .setDestinationRouteTargetConstrain(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
                                .xml.ns.yang.bgp.route.target.constrain.rev180618.update
                                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                .destination.route.target.constrain
                                .withdrawn._case.DestinationRouteTargetConstrainBuilder()
                                .setRouteTargetConstrainDestination(parseNlriDestinations(nlri, mPathSupported))
                                .build()).build()).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final Attributes1 pathAttributes1 = pathAttributes.augmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.augmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType()
                    instanceof DestinationRouteTargetConstrainAdvertizedCase) {
                final DestinationRouteTargetConstrainAdvertizedCase reach
                        = (DestinationRouteTargetConstrainAdvertizedCase) routes.getDestinationType();
                byteAggregator.writeBytes(serializeNlriDestinations(reach.getDestinationRouteTargetConstrain()
                        .getRouteTargetConstrainDestination()));
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null
                    && withdrawnRoutes.getDestinationType() instanceof DestinationRouteTargetConstrainWithdrawnCase) {
                final DestinationRouteTargetConstrainWithdrawnCase reach
                        = (DestinationRouteTargetConstrainWithdrawnCase) withdrawnRoutes.getDestinationType();
                byteAggregator.writeBytes(serializeNlriDestinations(reach.getDestinationRouteTargetConstrain()
                        .getRouteTargetConstrainDestination()));
            }
        }
    }
}
