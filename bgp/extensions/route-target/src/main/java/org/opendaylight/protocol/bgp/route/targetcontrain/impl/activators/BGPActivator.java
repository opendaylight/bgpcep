/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.RouteTargetConstrainNlriHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.RouteTargetConstrainRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCase;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    @VisibleForTesting
    static final int RT_SAFI = 132;

    private static void registerNlri(
            final BGPExtensionProviderContext context,
            final List<AutoCloseable> regs) {
        final RouteTargetConstrainNlriHandler routeTargetNlriHandler = new RouteTargetConstrainNlriHandler();
        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, RouteTargetConstrainSubsequentAddressFamily.class,
                routeTargetNlriHandler, ipv4NextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(RouteTargetConstrainRoutes.class, routeTargetNlriHandler));
    }

    private static void registerAfiSafi(final BGPExtensionProviderContext context, final List<AutoCloseable> regs) {
        regs.add(context.registerSubsequentAddressFamily(RouteTargetConstrainSubsequentAddressFamily.class, RT_SAFI));
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        NlriActivator.registerNlriParsers(regs);
        registerAfiSafi(context, regs);
        registerNlri(context, regs);
        return regs;
    }
}
