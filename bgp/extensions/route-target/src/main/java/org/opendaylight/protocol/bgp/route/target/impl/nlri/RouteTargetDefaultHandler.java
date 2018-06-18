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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetDefaultCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.RouteTargetDefaultCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.route.target.choice.route.target._default._case.RouteTargetDefaultRouteBuilder;

/**
 * Default Route Target nlri.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetDefaultHandler extends AbstractRouteTargetHandler<RouteTargetDefaultCase> {
    private static final RouteTargetDefaultCase DEFAULT = new RouteTargetDefaultCaseBuilder()
            .setRouteTargetDefaultRoute(new RouteTargetDefaultRouteBuilder().build()).build();
    private static final ByteBuf EMPTY = Unpooled.buffer();

    @Override
    public RouteTargetDefaultCase parseRouteTarget(final ByteBuf buffer) {
        return DEFAULT;
    }

    @Override
    public Integer getType() {
        return null;
    }

    @Override
    public ByteBuf serializeRouteTarget(final RouteTargetDefaultCase routeTarget) {
        return EMPTY;
    }

    @Override
    public Class<RouteTargetDefaultCase> getClazz() {
        return RouteTargetDefaultCase.class;
    }
}
