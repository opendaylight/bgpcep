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
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.As4RouteTargetExtendedHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetAs4ExtendedCommunityCaseBuilder;

/**
 * Route Target Nlri type 2.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetAS4OctetRouteHandler extends AbstractRouteTargetHandler<RouteTargetAs4ExtendedCommunityCase> {
    private static final int TYPE = 2;

    @Override
    public RouteTargetAs4ExtendedCommunityCase parseRouteTarget(final ByteBuf buffer) {
        return new RouteTargetAs4ExtendedCommunityCaseBuilder()
                .setAs4RouteTargetExtendedCommunity(As4RouteTargetExtendedHandler.parse(buffer))
                .build();
    }

    @Override
    public Integer getType() {
        return TYPE;
    }


    @Override
    public ByteBuf serializeRouteTarget(final RouteTargetAs4ExtendedCommunityCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        As4RouteTargetExtendedHandler.serialize(routeTarget.getAs4RouteTargetExtendedCommunity(), byteAggregator);
        return byteAggregator;
    }

    @Override
    public Class<RouteTargetAs4ExtendedCommunityCase> getClazz() {
        return RouteTargetAs4ExtendedCommunityCase.class;
    }
}
