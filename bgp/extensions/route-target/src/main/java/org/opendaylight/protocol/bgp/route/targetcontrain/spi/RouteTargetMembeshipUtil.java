/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainDefaultCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainIpv4RouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.route.target.constrain.choice.RouteTargetConstrainRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteTarget;

public final class RouteTargetMembeshipUtil {
    private RouteTargetMembeshipUtil() {
        // Hidden on purpose
    }

    public static RouteTarget getRT(final RouteTargetConstrainRoute route) {
        final RouteTargetConstrainChoice rtc = route.getRouteTargetConstrainChoice();
        if (rtc instanceof RouteTargetConstrainDefaultCase) {
            return ((RouteTargetConstrainDefaultCase) rtc).getRouteTargetConstrainDefaultRoute();
        } else if (rtc instanceof RouteTargetConstrainAs4ExtendedCommunityCase) {
            return ((RouteTargetConstrainAs4ExtendedCommunityCase) rtc).getAs4RouteTargetExtendedCommunity();
        } else if (rtc instanceof RouteTargetConstrainIpv4RouteCase) {
            return ((RouteTargetConstrainIpv4RouteCase) rtc).getRouteTargetIpv4();
        } else {
            return ((RouteTargetConstrainRouteCase) rtc).getRouteTargetExtendedCommunity();
        }
    }
}
