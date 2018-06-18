/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.As4RouteTargetExtendedHandler;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCaseBuilder;

/**
 * Route Target Nlri type 2.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetAS4OctetRouteHandler
        implements RouteTargetHandler<RouteTargetConstrainAs4ExtendedCommunityCase> {
    private static final int TYPE = 2;

    @Override
    public RouteTargetConstrainAs4ExtendedCommunityCase parseRouteTargetConstrain(final ByteBuf buffer) {
        return new RouteTargetConstrainAs4ExtendedCommunityCaseBuilder()
                .setAs4RouteTargetExtendedCommunity(As4RouteTargetExtendedHandler.parse(buffer))
                .build();
    }

    @Override
    public Integer getType() {
        return TYPE;
    }


    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainAs4ExtendedCommunityCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        As4RouteTargetExtendedHandler.serialize(routeTarget.getAs4RouteTargetExtendedCommunity(), byteAggregator);
        return byteAggregator;
    }

    @Override
    public Class<RouteTargetConstrainAs4ExtendedCommunityCase> getClazz() {
        return RouteTargetConstrainAs4ExtendedCommunityCase.class;
    }
}
