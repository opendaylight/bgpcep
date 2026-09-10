/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.policy.RTCCache;
import org.opendaylight.protocol.bgp.route.targetcontrain.spi.ClientRouteTargetContrainCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;

public final class RTCClientRouteCache implements ClientRouteTargetContrainCache, RTCCache {
    @GuardedBy("this")
    private final ArrayList<RouteTargetConstrainRoute> rtCache = new ArrayList<>();

    @Override
    public synchronized void cacheRoute(final Route route) {
        if (route instanceof RouteTargetConstrainRoute rt) {
            rtCache.add(rt);
        }
    }

    @Override
    public synchronized void uncacheRoute(final Route route) {
        if (route instanceof RouteTargetConstrainRoute rt) {
            rtCache.remove(rt);
        }
    }

    @Override
    public synchronized List<Route> getClientRouteTargetContrainCache() {
        return ImmutableList.copyOf(rtCache);
    }
}
