/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetExtendedCommunityHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetRouteCaseBuilder;

/**
 * Route Target Nlri type 0.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetASRouteHandler extends AbstractRouteTargetHandler<RouteTargetRouteCase> {
    private static final int TYPE = 0;

    @Override
    public RouteTargetRouteCase parseRouteTarget(final ByteBuf buffer) {
        return new RouteTargetRouteCaseBuilder()
                .setRouteTargetExtendedCommunity(RouteTargetExtendedCommunityHandler.parse(buffer))
                .build();
    }

    @Override
    public Integer getType() {
        return TYPE;
    }

    @Override
    public ByteBuf serializeRouteTarget(final RouteTargetRouteCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        RouteTargetExtendedCommunityHandler.serialize(routeTarget.getRouteTargetExtendedCommunity(), byteAggregator);
        return byteAggregator;
    }

    @Override
    public Class<RouteTargetRouteCase> getClazz() {
        return RouteTargetRouteCase.class;
    }
}
