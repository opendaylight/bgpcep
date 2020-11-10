/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargeConstraintNlriRegistry;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainParser;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCase;

/**
 * Route Target Nlri Registry.
 *
 * @author Claudio D. Gasparini
 */
public final class ImmutableRouteTargetConstrainNlriRegistry implements RouteTargeConstraintNlriRegistry {
    private static final @NonNull ImmutableRouteTargetConstrainNlriRegistry INSTANCE =
            new ImmutableRouteTargetConstrainNlriRegistry();
    private static final short RT_SUBTYPE = 2;

    @SuppressWarnings("rawtypes")
    private final RouteTargetConstrainParser[] parsers = new RouteTargetConstrainParser[3];
    private final DefaultRouteTargetConstrainParser defaultParser = new DefaultRouteTargetConstrainParser();
    private final ImmutableMap<Class<? extends RouteTargetConstrainChoice>,
        RouteTargetConstrainSerializer<RouteTargetConstrainChoice>> serializers;

    // Warnings due to builder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ImmutableRouteTargetConstrainNlriRegistry() {
        final ImmutableMap.Builder builder = ImmutableMap.builder();
        final var routeTargetASRouteHandler = new RouteTargetASRouteHandler();
        parsers[routeTargetASRouteHandler.getType()] = routeTargetASRouteHandler;
        builder.put(RouteTargetConstrainRouteCase.class, routeTargetASRouteHandler);

        final var routeTargetIpv4RouteHandler = new RouteTargetIpv4RouteHandler();
        parsers[routeTargetIpv4RouteHandler.getType()] = routeTargetIpv4RouteHandler;
        builder.put(RouteTargetConstrainIpv4RouteCase.class, routeTargetIpv4RouteHandler);

        final var targetAS4OctetRouteHandler = new RouteTargetAS4OctetRouteHandler();
        parsers[targetAS4OctetRouteHandler.getType()] = targetAS4OctetRouteHandler;
        builder.put(RouteTargetConstrainAs4ExtendedCommunityCase.class, targetAS4OctetRouteHandler);

        serializers = builder.build();
        // We rely on parsers being fully populated below
        Arrays.stream(parsers).forEach(Verify::verifyNotNull);
    }

    public static @NonNull ImmutableRouteTargetConstrainNlriRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public RouteTargetConstrainChoice parseRouteTargetConstrain(final Integer type, final ByteBuf nlriBuf) {
        checkArgument(nlriBuf != null && (nlriBuf.isReadable() || type == null && !nlriBuf.isReadable()),
                "Array of bytes is mandatory. Can't be null or empty.");
        final RouteTargetConstrainParser<?> parser;
        if (type != null) {
            final int offset = type;
            if (offset < 0 || offset > parsers.length) {
                return null;
            }
            parser = parsers[offset];
        } else {
            parser = defaultParser;
        }
        return parser.parseRouteTargetConstrain(nlriBuf);
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
}
