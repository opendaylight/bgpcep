/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import java.util.List;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.RouteTargetAS4OctetRouteHandler;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.RouteTargetASRouteHandler;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.RouteTargetIpv4RouteHandler;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.SimpleRouteTargetConstrainNlriRegistry;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Nlri Registry activator.
 *
 * @author Claudio D. Gasparini
 */
public final class NlriActivator {
    private NlriActivator() {
        // Hidden on purpose
    }

    public static void registerNlriParsers(final List<Registration> regs) {
        final SimpleRouteTargetConstrainNlriRegistry nlriRegistry
                = SimpleRouteTargetConstrainNlriRegistry.getInstance();

        final RouteTargetASRouteHandler routeTargetRouteHandler = new RouteTargetASRouteHandler();
        regs.add(nlriRegistry.registerNlriParser(routeTargetRouteHandler));
        regs.add(nlriRegistry.registerNlriSerializer(routeTargetRouteHandler));

        final RouteTargetIpv4RouteHandler routeTargetIpv4RouteHandler = new RouteTargetIpv4RouteHandler();
        regs.add(nlriRegistry.registerNlriParser(routeTargetIpv4RouteHandler));
        regs.add(nlriRegistry.registerNlriSerializer(routeTargetIpv4RouteHandler));

        final RouteTargetAS4OctetRouteHandler targetAS4OctetRouteHandler = new RouteTargetAS4OctetRouteHandler();
        regs.add(nlriRegistry.registerNlriParser(targetAS4OctetRouteHandler));
        regs.add(nlriRegistry.registerNlriSerializer(targetAS4OctetRouteHandler));
    }
}
