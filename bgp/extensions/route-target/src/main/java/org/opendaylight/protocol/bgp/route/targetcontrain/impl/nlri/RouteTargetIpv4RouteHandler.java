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
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target.RouteTargetIpv4Handler;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCase;

/**
 * Route Target Nlri type 1.
 *
 * @author Claudio D. Gasparini
 */
final class RouteTargetIpv4RouteHandler implements RouteTargetConstrainSerializer<RouteTargetConstrainIpv4RouteCase> {
    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public ByteBuf serializeRouteTargetConstrain(final RouteTargetConstrainIpv4RouteCase routeTarget) {
        final ByteBuf byteAggregator = Unpooled.buffer();
        RouteTargetIpv4Handler.serialize(routeTarget.getRouteTargetIpv4(), byteAggregator);
        return byteAggregator;
    }
}
