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
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetIpv4Handler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetIpv4RouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetIpv4RouteCaseBuilder;

/**
 * Route Target Nlri type 1.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetIpv4RouteHandler extends AbstractRouteTargetHandler<RouteTargetIpv4RouteCase> {
    private static final int TYPE = 1;

    @Override
    public RouteTargetIpv4RouteCase parseRouteTarget(final ByteBuf buffer) {
        return new RouteTargetIpv4RouteCaseBuilder()
                .setRouteTargetIpv4(RouteTargetIpv4Handler.parse(buffer)).build();
    }

    @Override
    public Integer getType() {
        return TYPE;
    }


    @Override
    public ByteBuf serializeRouteTarget(final RouteTargetIpv4RouteCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        RouteTargetIpv4Handler.serialize(routeTarget.getRouteTargetIpv4(), byteAggregator);
        return byteAggregator;
    }

    @Override
    public Class<RouteTargetIpv4RouteCase> getClazz() {
        return RouteTargetIpv4RouteCase.class;
    }
}
