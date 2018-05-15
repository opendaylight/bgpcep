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
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community.SourceAS4OctectHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community.SourceASHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community.VrfRouteImportHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.MvpnNlriHandler;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAsExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCase;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    @VisibleForTesting
    static final int MVPN_SAFI = 5;

    private static void registerAttributesHandler(
            final BGPExtensionProviderContext context,
            final List<AutoCloseable> regs) {
        final PEDistinguisherLabelsAttributeHandler peDistHandler =
                new PEDistinguisherLabelsAttributeHandler();
        regs.add(context.registerAttributeParser(peDistHandler.getType(), peDistHandler));
        regs.add(context.registerAttributeSerializer(peDistHandler.getClazz(), peDistHandler));

        final PMSITunnelAttributeHandler pmsiParser = new PMSITunnelAttributeHandler();
        regs.add(context.registerAttributeParser(pmsiParser.getType(), pmsiParser));
        regs.add(context.registerAttributeSerializer(pmsiParser.getClazz(), pmsiParser));
    }

    private static void registerNlri(
            final BGPExtensionProviderContext context,
            final List<AutoCloseable> regs) {
        final MvpnNlriHandler mvpnNlriHandler = new MvpnNlriHandler();
        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                mvpnNlriHandler, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                mvpnNlriHandler, ipv6NextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(MvpnRoutes.class, mvpnNlriHandler));

    }

    private static void registerExtendedCommunities(final BGPExtensionProviderContext context,
            final List<AutoCloseable> regs) {

        final VrfRouteImportHandler vrfRouteImportHandler = new VrfRouteImportHandler();
        regs.add(context.registerExtendedCommunityParser(vrfRouteImportHandler.getType(true),
                vrfRouteImportHandler.getSubType(), vrfRouteImportHandler));
        regs.add(context.registerExtendedCommunitySerializer(SourceAsExtendedCommunityCase.class,
                vrfRouteImportHandler));

        final SourceAS4OctectHandler source4ASHandler = new SourceAS4OctectHandler();
        regs.add(context.registerExtendedCommunityParser(source4ASHandler.getType(true),
                source4ASHandler.getSubType(), source4ASHandler));
        regs.add(context.registerExtendedCommunitySerializer(SourceAs4ExtendedCommunityCase.class, source4ASHandler));

        final SourceASHandler sourceASHandler = new SourceASHandler();
        regs.add(context.registerExtendedCommunityParser(sourceASHandler.getType(true),
                sourceASHandler.getSubType(), sourceASHandler));
        regs.add(context.registerExtendedCommunitySerializer(SourceAsExtendedCommunityCase.class, sourceASHandler));
    }

    private static void registerAfiSafi(final BGPExtensionProviderContext context, final List<AutoCloseable> regs) {
        regs.add(context.registerSubsequentAddressFamily(McastVpnSubsequentAddressFamily.class, MVPN_SAFI));
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        TunnelIdentifierActivator.registerTunnelIdentifierHandlers(context, regs);
        registerAfiSafi(context, regs);
        registerNlri(context, regs);
        registerExtendedCommunities(context, regs);
        registerAttributesHandler(context, regs);
        NlriActivator.registerNlriParsers(regs);
        return regs;
    }
}
