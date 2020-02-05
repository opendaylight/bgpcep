/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.PEDistinguisherLabelsAttributeHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.PMSITunnelAttributeHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.MvpnIpv4NlriHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.MvpnIpv6NlriHandler;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.routes.ipv4.MvpnRoutesIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.routes.ipv6.MvpnRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    @VisibleForTesting
    static final int MVPN_SAFI = 5;

    private static void registerAttributesHandler(final BGPExtensionProviderContext context,
            final List<Registration> regs) {
        final PEDistinguisherLabelsAttributeHandler peDistHandler =
                new PEDistinguisherLabelsAttributeHandler();
        regs.add(context.registerAttributeParser(peDistHandler.getType(), peDistHandler));
        regs.add(context.registerAttributeSerializer(peDistHandler.getClazz(), peDistHandler));

        final PMSITunnelAttributeHandler pmsiParser = new PMSITunnelAttributeHandler();
        regs.add(context.registerAttributeParser(pmsiParser.getType(), pmsiParser));
        regs.add(context.registerAttributeSerializer(pmsiParser.getClazz(), pmsiParser));
    }

    private static void registerNlri(final BGPExtensionProviderContext context, final List<Registration> regs) {
        final MvpnIpv4NlriHandler mvpnIpv4NlriHandler = new MvpnIpv4NlriHandler();
        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                mvpnIpv4NlriHandler, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriSerializer(MvpnRoutesIpv4.class, mvpnIpv4NlriHandler));


        final MvpnIpv6NlriHandler mvpnIpv6NlriHandler = new MvpnIpv6NlriHandler();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                mvpnIpv6NlriHandler, ipv6NextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(MvpnRoutesIpv6.class, mvpnIpv6NlriHandler));
    }

    private static void registerAfiSafi(final BGPExtensionProviderContext context, final List<Registration> regs) {
        regs.add(context.registerSubsequentAddressFamily(McastVpnSubsequentAddressFamily.class, MVPN_SAFI));
    }

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();
        TunnelIdentifierActivator.registerTunnelIdentifierHandlers(context, regs);
        registerAfiSafi(context, regs);
        registerNlri(context, regs);
        registerAttributesHandler(context, regs);
        NlriActivator.registerNlriParsers(regs);
        return regs;
    }
}
