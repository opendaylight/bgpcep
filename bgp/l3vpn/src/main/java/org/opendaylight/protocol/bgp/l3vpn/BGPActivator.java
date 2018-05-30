/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.routes.VpnIpv6Routes;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    private static void registerNlri(
            final BGPExtensionProviderContext context,
            final List<AutoCloseable> regs) {
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
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        registerNlri(context, regs);
        return regs;
    }
}
