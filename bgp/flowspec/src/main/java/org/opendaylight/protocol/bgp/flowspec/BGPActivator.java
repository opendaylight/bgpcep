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
import org.opendaylight.protocol.bgp.flowspec.nex.thop.FlowspecIpv4NextHopParser;
import org.opendaylight.protocol.bgp.flowspec.nex.thop.FlowspecIpv6NextHopParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.c.next.hop.FlowspecIpv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.c.next.hop.FlowspecIpv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int FLOWSPEC_SAFI = 133;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerSubsequentAddressFamily(FlowspecSubsequentAddressFamily.class, FLOWSPEC_SAFI));

        final FSIpv4NlriParser ipv4Handler = new FSIpv4NlriParser();
        final FSIpv6NlriParser ipv6Handler = new FSIpv6NlriParser();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class, ipv4Handler));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class, ipv6Handler));
        regs.add(context.registerNlriSerializer(FlowspecRoutes.class, ipv4Handler));
        regs.add(context.registerNlriSerializer(FlowspecIpv6Routes.class, ipv6Handler));

        final ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new FSExtendedCommunitiesAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(ExtendedCommunities.class, extendedCommunitiesAttributeParser));
        regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, extendedCommunitiesAttributeParser));

        FlowspecIpv4NextHopParser flowspecIpv4NextHopParser = new FlowspecIpv4NextHopParser();
        context.registerNextHopParser(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class, flowspecIpv4NextHopParser);
        context.registerNextHopSerializer(FlowspecIpv4NextHopCase.class, flowspecIpv4NextHopParser);

        FlowspecIpv6NextHopParser flowspecIpv6NextHopParser = new FlowspecIpv6NextHopParser();
        context.registerNextHopParser(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class, flowspecIpv6NextHopParser);
        context.registerNextHopSerializer(FlowspecIpv6NextHopCase.class, flowspecIpv6NextHopParser);

        return regs;
    }
}
