/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.mcast.nlri.L3vpnMcastIpv4NlriHandler;
import org.opendaylight.protocol.bgp.l3vpn.mcast.nlri.L3vpnMcastIpv6NlriHandler;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv4.L3vpnMcastRoutesIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.ipv6.L3vpnMcastRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.routes.VpnIpv6Routes;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    @VisibleForTesting
    static final int MCAST_L3VPN_SAFI = 129;

    private static void registerNlri(
            final BGPExtensionProviderContext context,
            final List<Registration> regs) {
        final VpnIpv4NlriParser vpnIpv4NlriParser = new VpnIpv4NlriParser();
        final VpnIpv4NextHopParserSerializer vpnIpv4NextHopParserSerializer = new VpnIpv4NextHopParserSerializer();

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                vpnIpv4NlriParser, vpnIpv4NextHopParserSerializer, Ipv4NextHopCase.class));
        regs.add(context.registerNlriSerializer(VpnIpv4Routes.class, vpnIpv4NlriParser));

        final VpnIpv6NlriParser vpnIpv6NlriParser = new VpnIpv6NlriParser();
        final VpnIpv6NextHopParserSerializer vpnIpv6NextHopParserSerializer = new VpnIpv6NextHopParserSerializer();

        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                vpnIpv6NlriParser, vpnIpv6NextHopParserSerializer, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(VpnIpv6Routes.class, vpnIpv6NlriParser));

        final L3vpnMcastIpv4NlriHandler l3vpnMcastIpv4NlriHandler = new L3vpnMcastIpv4NlriHandler();
        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class,
                l3vpnMcastIpv4NlriHandler, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriSerializer(L3vpnMcastRoutesIpv4.class, l3vpnMcastIpv4NlriHandler));


        final L3vpnMcastIpv6NlriHandler l3vpnMcastIpv6NlriHandler = new L3vpnMcastIpv6NlriHandler();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class,
                l3vpnMcastIpv6NlriHandler, ipv6NextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(L3vpnMcastRoutesIpv6.class, l3vpnMcastIpv6NlriHandler));
    }

    private static void registerAfiSafi(final BGPExtensionProviderContext context, final List<Registration> regs) {
        regs.add(context.registerSubsequentAddressFamily(McastMplsLabeledVpnSubsequentAddressFamily.class,
                MCAST_L3VPN_SAFI));
    }

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();
        registerAfiSafi(context, regs);
        registerNlri(context, regs);
        return regs;
    }
}
