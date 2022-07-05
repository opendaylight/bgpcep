/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.mcast.nlri.L3vpnMcastIpv4NlriHandler;
import org.opendaylight.protocol.bgp.l3vpn.mcast.nlri.L3vpnMcastIpv6NlriHandler;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
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
import org.osgi.service.component.annotations.Component;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.l3vpn.BGPActivator")
@MetaInfServices
public final class BGPActivator implements BGPExtensionProviderActivator {
    @VisibleForTesting
    static final int MCAST_L3VPN_SAFI = 129;

    @Inject
    public BGPActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final BGPExtensionProviderContext context) {
        final VpnIpv4NlriParser vpnIpv4NlriParser = new VpnIpv4NlriParser();
        final VpnIpv6NlriParser vpnIpv6NlriParser = new VpnIpv6NlriParser();
        final L3vpnMcastIpv4NlriHandler l3vpnMcastIpv4NlriHandler = new L3vpnMcastIpv4NlriHandler();
        final L3vpnMcastIpv6NlriHandler l3vpnMcastIpv6NlriHandler = new L3vpnMcastIpv6NlriHandler();

        return List.of(
            context.registerSubsequentAddressFamily(McastMplsLabeledVpnSubsequentAddressFamily.VALUE, MCAST_L3VPN_SAFI),
            context.registerNlriParser(Ipv4AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE,
                vpnIpv4NlriParser, new VpnIpv4NextHopParserSerializer(), Ipv4NextHopCase.class),
            context.registerNlriSerializer(VpnIpv4Routes.class, vpnIpv4NlriParser),
            context.registerNlriParser(Ipv6AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE,
                vpnIpv6NlriParser, new VpnIpv6NextHopParserSerializer(), Ipv6NextHopCase.class),
            context.registerNlriSerializer(VpnIpv6Routes.class, vpnIpv6NlriParser),
            context.registerNlriParser(Ipv4AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE,
                l3vpnMcastIpv4NlriHandler, new Ipv4NextHopParserSerializer(), Ipv4NextHopCase.class),
            context.registerNlriSerializer(L3vpnMcastRoutesIpv4.class, l3vpnMcastIpv4NlriHandler),
            context.registerNlriParser(Ipv6AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE,
                l3vpnMcastIpv6NlriHandler, new Ipv6NextHopParserSerializer(), Ipv6NextHopCase.class),
            context.registerNlriSerializer(L3vpnMcastRoutesIpv6.class, l3vpnMcastIpv6NlriHandler));
    }
}
