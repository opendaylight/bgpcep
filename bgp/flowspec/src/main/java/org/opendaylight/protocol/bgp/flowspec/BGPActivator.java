/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectAsFourOctetEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectAsTwoOctetEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectIpNextHopEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectIpv4EcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.RedirectIpv6EcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficActionEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficMarkingEcHandler;
import org.opendaylight.protocol.bgp.flowspec.extended.communities.TrafficRateEcHandler;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6.FlowspecL3vpnIpv6NlriParser;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.route.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int FLOWSPEC_SAFI = 133;
    private static final int FLOWSPEC_L3VPN_SAFI = 134;

    private final FlowspecActivator activator;

    public BGPActivator(@Nonnull final FlowspecActivator activator) {
        this.activator = requireNonNull(activator);
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        final SimpleFlowspecExtensionProviderContext flowspecContext = this.activator.getContext();

        regs.add(context.registerSubsequentAddressFamily(FlowspecSubsequentAddressFamily.class, FLOWSPEC_SAFI));
        regs.add(context.registerSubsequentAddressFamily(FlowspecL3vpnSubsequentAddressFamily.class, FLOWSPEC_L3VPN_SAFI));

        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();

        final SimpleFlowspecIpv4NlriParser fsIpv4Handler = new SimpleFlowspecIpv4NlriParser(flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC));
        final SimpleFlowspecIpv6NlriParser fsIpv6Handler = new SimpleFlowspecIpv6NlriParser(flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC));
        regs.add(
                context.registerNlriParser(
                        Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class, fsIpv4Handler, ipv4NextHopParser, Ipv4NextHopCase.class
                        )
                );
        regs.add(
                context.registerNlriParser(
                        Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class, fsIpv6Handler, ipv6NextHopParser, Ipv6NextHopCase.class
                        )
                );
        regs.add(context.registerNlriSerializer(FlowspecRoutes.class, fsIpv4Handler));
        regs.add(context.registerNlriSerializer(FlowspecIpv6Routes.class, fsIpv6Handler));

        final FlowspecL3vpnIpv4NlriParser fsL3vpnIpv4Handler = new FlowspecL3vpnIpv4NlriParser(flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));
        final FlowspecL3vpnIpv6NlriParser fsL3vpnIpv6Handler = new FlowspecL3vpnIpv6NlriParser(flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV6, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));
        regs.add(
                context.registerNlriParser(
                        Ipv4AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class, fsL3vpnIpv4Handler, ipv4NextHopParser, Ipv4NextHopCase.class
                        )
                );
        regs.add(
                context.registerNlriParser(
                        Ipv6AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class, fsL3vpnIpv6Handler, ipv6NextHopParser, Ipv6NextHopCase.class
                        )
                );
        regs.add(context.registerNlriSerializer(FlowspecL3vpnIpv4Routes.class, fsL3vpnIpv4Handler));
        regs.add(context.registerNlriSerializer(FlowspecL3vpnIpv6Routes.class, fsL3vpnIpv6Handler));

        final RedirectAsTwoOctetEcHandler redirect2bHandler = new RedirectAsTwoOctetEcHandler();
        regs.add(context.registerExtendedCommunityParser(redirect2bHandler.getType(true), redirect2bHandler.getSubType(), redirect2bHandler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectExtendedCommunityCase.class, redirect2bHandler));

        final TrafficActionEcHandler trafficActionHandler = new TrafficActionEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficActionHandler.getType(true), trafficActionHandler.getSubType(), trafficActionHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficActionExtendedCommunityCase.class, trafficActionHandler));

        final TrafficMarkingEcHandler trafficMarkingHandler = new TrafficMarkingEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficMarkingHandler.getType(true), trafficMarkingHandler.getSubType(), trafficMarkingHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficMarkingExtendedCommunityCase.class, trafficMarkingHandler));

        final TrafficRateEcHandler trafficRateEcHandler = new TrafficRateEcHandler();
        regs.add(context.registerExtendedCommunityParser(trafficRateEcHandler.getType(true), trafficRateEcHandler.getSubType(), trafficRateEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(TrafficRateExtendedCommunityCase.class, trafficRateEcHandler));

        final RedirectIpv6EcHandler redirectIpv6EcHandler = new RedirectIpv6EcHandler();
        regs.add(context.registerExtendedCommunityParser(redirectIpv6EcHandler.getType(true), redirectIpv6EcHandler.getSubType(), redirectIpv6EcHandler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectIpv6ExtendedCommunityCase.class, redirectIpv6EcHandler));

        final RedirectAsFourOctetEcHandler redirect4bHandler = new RedirectAsFourOctetEcHandler();
        regs.add(context.registerExtendedCommunityParser(redirect4bHandler.getType(true), redirect4bHandler.getSubType(), redirect4bHandler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectAs4ExtendedCommunityCase.class, redirect4bHandler));

        final RedirectIpv4EcHandler redirectIpv4Handler = new RedirectIpv4EcHandler();
        regs.add(context.registerExtendedCommunityParser(redirectIpv4Handler.getType(true), redirectIpv4Handler.getSubType(), redirectIpv4Handler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectIpv4ExtendedCommunityCase.class, redirectIpv4Handler));

        final RedirectIpNextHopEcHandler redirectIpNhHandler = new RedirectIpNextHopEcHandler();
        regs.add(context.registerExtendedCommunityParser(redirectIpNhHandler.getType(true), redirectIpNhHandler.getSubType(), redirectIpNhHandler));
        regs.add(context.registerExtendedCommunitySerializer(RedirectIpNhExtendedCommunityCase.class, redirectIpNhHandler));

        return regs;
    }
}
