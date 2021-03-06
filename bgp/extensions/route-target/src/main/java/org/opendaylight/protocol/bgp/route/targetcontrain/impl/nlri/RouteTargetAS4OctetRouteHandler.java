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
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCase;

/**
 * Route Target Nlri type 2.
 *
 * @author Claudio D. Gasparini
 */
final class RouteTargetAS4OctetRouteHandler
        implements RouteTargetConstrainSerializer<RouteTargetConstrainAs4ExtendedCommunityCase> {
    @Override
    public byte getType() {
        return 2;
    }

    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainAs4ExtendedCommunityCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        As4RouteTargetExtendedHandler.serialize(routeTarget.getAs4RouteTargetExtendedCommunity(), byteAggregator);
        return byteAggregator;
    }
}
