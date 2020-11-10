/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri.RouteTargetConstrainParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.constrain._default.route.grouping.RouteTargetConstrainDefaultRouteBuilder;

/**
 * Default Route Target nlri.
 *
 * @author Claudio D. Gasparini
 */
@NonNullByDefault
final class DefaultRouteTargetConstrainParser implements RouteTargetConstrainParser<RouteTargetConstrainDefaultCase> {
    private static final RouteTargetConstrainDefaultCase DEFAULT = new RouteTargetConstrainDefaultCaseBuilder()
            .setRouteTargetConstrainDefaultRoute(new RouteTargetConstrainDefaultRouteBuilder().build())
            .build();

    @Override
    public RouteTargetConstrainDefaultCase parseRouteTargetConstrain(final ByteBuf buffer) {
        return DEFAULT;
    }
}
