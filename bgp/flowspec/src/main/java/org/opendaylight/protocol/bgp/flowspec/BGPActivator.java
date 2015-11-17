/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectAsTwoOctetEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficActionEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficMarkingEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficRateEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.next.hop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.next.hop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.flowspec.routes.flowspec.route.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.flowspec.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.flowspec.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.flowspec.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int FLOWSPEC_SAFI = 133;

    private final FlowspecActivator fsActivator = new FlowspecActivator();
    private final SimpleFlowspecExtensionProviderContext flowspecContext = new SimpleFlowspecExtensionProviderContext();

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {

        final List<AutoCloseable> regs = new ArrayList<>();

        this.fsActivator.start(this.flowspecContext);

        regs.add(context.registerSubsequentAddressFamily(FlowspecSubsequentAddressFamily.class, FLOWSPEC_SAFI));

        final SimpleFlowspecIpv4NlriParser fsIpv4Handler = new SimpleFlowspecIpv4NlriParser(this.flowspecContext.getFlowspecIpv4TypeRegistry());
        final SimpleFlowspecIpv6NlriParser fsIpv6Handler = new SimpleFlowspecIpv6NlriParser(this.flowspecContext.getFlowspecIpv6TypeRegistry());

        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class,
            fsIpv4Handler, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class,
            fsIpv6Handler, ipv6NextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(FlowspecRoutes.class, fsIpv4Handler));
        regs.add(context.registerNlriSerializer(FlowspecIpv6Routes.class, fsIpv6Handler));

        final RedirectAsTwoOctetEcHandler redirect2bHandler = new RedirectAsTwoOctetEcHandler();
        regs.add(context.registerExtendedCommunityParser(redirect2bHandler.getType(true), redirect2bHandler.getSubType(), redirect2bHandler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectExtendedCommunityCase.class, redirect2bHandler));
        regs.add(context.registerExtendedCommunitySerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.flowspec.ipv6.routes.flowspec.route.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase.class, redirect2bHandler));

        final TrafficActionEcHandler trafficActionHandler = new TrafficActionEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficActionHandler.getType(true), trafficActionHandler.getSubType(), trafficActionHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficActionExtendedCommunityCase.class, trafficActionHandler));
        regs.add(context.registerExtendedCommunitySerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.flowspec.ipv6.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase.class, trafficActionHandler));

        final TrafficMarkingEcHandler trafficMarkingHandler = new TrafficMarkingEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficMarkingHandler.getType(true), trafficMarkingHandler.getSubType(), trafficMarkingHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficMarkingExtendedCommunityCase.class, trafficMarkingHandler));
        regs.add(context.registerExtendedCommunitySerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.flowspec.ipv6.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase.class, trafficMarkingHandler));

        final TrafficRateEcHandler trafficRateEcHandler = new TrafficRateEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficRateEcHandler.getType(true), trafficRateEcHandler.getSubType(), trafficRateEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficRateExtendedCommunityCase.class, trafficRateEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.flowspec.ipv6.routes.flowspec.route.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase.class, trafficRateEcHandler));

        return regs;
    }

    public void stopFlowspecActivator() {
        this.fsActivator.stop();
    }
}
