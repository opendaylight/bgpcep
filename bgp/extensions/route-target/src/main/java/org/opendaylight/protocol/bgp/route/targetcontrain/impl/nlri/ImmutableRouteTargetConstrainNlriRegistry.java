/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.As4RouteTargetExtendedHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetExtendedCommunityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetIpv4Handler;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargeConstraintNlriRegistry;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.constrain._default.route.grouping.RouteTargetConstrainDefaultRouteBuilder;

/**
 * Route Target Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class ImmutableRouteTargetConstrainNlriRegistry implements RouteTargeConstraintNlriRegistry {
    private static final @NonNull ImmutableRouteTargetConstrainNlriRegistry INSTANCE =
            new ImmutableRouteTargetConstrainNlriRegistry();
    // Default Route Target NLRI
    private static final RouteTargetConstrainDefaultCase DEFAULT = new RouteTargetConstrainDefaultCaseBuilder()
        .setRouteTargetConstrainDefaultRoute(new RouteTargetConstrainDefaultRouteBuilder().build())
        .build();
    private static final short RT_SUBTYPE = 2;

    private final ImmutableMap<Class<? extends RouteTargetConstrainChoice>,
        RouteTargetConstrainSerializer<RouteTargetConstrainChoice>> serializers;

    // Warnings due to builder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ImmutableRouteTargetConstrainNlriRegistry() {
        final ImmutableMap.Builder builder = ImmutableMap.builder();
        final var routeTargetASRouteHandler = new RouteTargetASRouteHandler();
        builder.put(RouteTargetConstrainRouteCase.class, routeTargetASRouteHandler);

        final var routeTargetIpv4RouteHandler = new RouteTargetIpv4RouteHandler();
        builder.put(RouteTargetConstrainIpv4RouteCase.class, routeTargetIpv4RouteHandler);

        final var targetAS4OctetRouteHandler = new RouteTargetAS4OctetRouteHandler();
        builder.put(RouteTargetConstrainAs4ExtendedCommunityCase.class, targetAS4OctetRouteHandler);

        serializers = builder.build();
    }

    public static @NonNull ImmutableRouteTargetConstrainNlriRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public RouteTargetConstrainChoice parseRouteTargetConstrain(final Integer type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && (nlriBuf.isReadable() || type == null && !nlriBuf.isReadable()),
                "Array of bytes is mandatory. Can't be null or empty.");
        return type == null ? DEFAULT : parseConstrain(type, nlriBuf);
    }

    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainChoice routeTarget) {
        final RouteTargetConstrainSerializer<RouteTargetConstrainChoice> serializer =
            serializers.get(routeTarget.implementedInterface());
        return serializer == null ? Unpooled.EMPTY_BUFFER : Unpooled.buffer()
                .writeByte(serializer.getType())
                .writeByte(RT_SUBTYPE)
                .writeBytes(serializer.serializeRouteTargetConstrain(routeTarget));
    }

    private static RouteTargetConstrainChoice parseConstrain(final int type, final ByteBuf nlriBuf) {
        switch (type) {
            case 0:
                return parseASRoute(nlriBuf);
            case 1:
                return parseIpv4Route(nlriBuf);
            case 2:
                return parseAS4OctetRoute(nlriBuf);
            default:
                return null;
        }
    }

    private static RouteTargetConstrainChoice parseASRoute(final ByteBuf buffer) {
        return new RouteTargetConstrainRouteCaseBuilder()
                .setRouteTargetExtendedCommunity(RouteTargetExtendedCommunityHandler.parse(buffer))
                .build();
    }

    private static RouteTargetConstrainChoice parseIpv4Route(final ByteBuf nlriBuf) {
        return new RouteTargetConstrainIpv4RouteCaseBuilder()
                .setRouteTargetIpv4(RouteTargetIpv4Handler.parse(nlriBuf)).build();
    }

    private static RouteTargetConstrainChoice parseAS4OctetRoute(final ByteBuf nlriBuf) {
        return new RouteTargetConstrainAs4ExtendedCommunityCaseBuilder()
            .setAs4RouteTargetExtendedCommunity(As4RouteTargetExtendedHandler.parse(nlriBuf))
            .build();
    }
}
