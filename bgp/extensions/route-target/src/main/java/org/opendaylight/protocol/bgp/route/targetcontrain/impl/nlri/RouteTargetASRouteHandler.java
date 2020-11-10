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
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetExtendedCommunityHandler;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCase;

/**
 * Route Target Nlri type 0.
 *
 * @author Claudio D. Gasparini
 */
final class RouteTargetASRouteHandler implements RouteTargetConstrainSerializer<RouteTargetConstrainRouteCase> {
    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainRouteCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        RouteTargetExtendedCommunityHandler.serialize(routeTarget.getRouteTargetExtendedCommunity(), byteAggregator);
        return byteAggregator;
    }
}
