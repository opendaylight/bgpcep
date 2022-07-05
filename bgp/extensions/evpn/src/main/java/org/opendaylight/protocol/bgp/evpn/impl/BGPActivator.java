/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.DefaultGatewayExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.ESILabelExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.ESImpRouteTargetExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.Layer2AttributesExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.MACMobExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.EvpnNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.EvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsImportRouteExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsiLabelExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.Layer2AttributesExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.evpn.impl.BGPActivator")
@MetaInfServices
public final class BGPActivator implements BGPExtensionProviderActivator {
    @VisibleForTesting
    static final int L2VPN_AFI = 25;
    @VisibleForTesting
    static final int EVPN_SAFI = 70;

    @Inject
    public BGPActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        regs.add(context.registerSubsequentAddressFamily(EvpnSubsequentAddressFamily.VALUE, EVPN_SAFI));
        regs.add(context.registerAddressFamily(L2vpnAddressFamily.VALUE, L2VPN_AFI));

        registerNlriHandler(context, regs);
        registerExtendedCommunities(context, regs);

        return regs;
    }

    private static void registerNlriHandler(final BGPExtensionProviderContext context, final List<Registration> regs) {
        final NextHopParserSerializer nextHopParser = new NextHopParserSerializer() {};
        final EvpnNlriParser nlriHandler = new EvpnNlriParser();
        regs.add(context.registerNlriParser(L2vpnAddressFamily.VALUE, EvpnSubsequentAddressFamily.VALUE,
            nlriHandler, nextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(EvpnRoutes.class, nlriHandler));
    }

    private static void registerExtendedCommunities(final BGPExtensionProviderContext context,
            final List<Registration> regs) {
        final DefaultGatewayExtCom defGEC = new DefaultGatewayExtCom();
        regs.add(context.registerExtendedCommunityParser(defGEC.getType(true), defGEC.getSubType(), defGEC));
        regs.add(context.registerExtendedCommunitySerializer(DefaultGatewayExtendedCommunityCase.class, defGEC));

        final ESILabelExtCom esiLEC = new ESILabelExtCom();
        regs.add(context.registerExtendedCommunityParser(esiLEC.getType(true), esiLEC.getSubType(), esiLEC));
        regs.add(context.registerExtendedCommunitySerializer(EsiLabelExtendedCommunityCase.class, esiLEC));

        final ESImpRouteTargetExtCom esImpEC = new ESImpRouteTargetExtCom();
        regs.add(context.registerExtendedCommunityParser(esImpEC.getType(true),
                esImpEC.getSubType(), esImpEC));
        regs.add(context.registerExtendedCommunitySerializer(EsImportRouteExtendedCommunityCase.class, esImpEC));

        final MACMobExtCom macEC = new MACMobExtCom();
        regs.add(context.registerExtendedCommunityParser(macEC.getType(true), macEC.getSubType(), macEC));
        regs.add(context.registerExtendedCommunitySerializer(MacMobilityExtendedCommunityCase.class, macEC));

        final Layer2AttributesExtCom l2a = new Layer2AttributesExtCom();
        regs.add(context.registerExtendedCommunityParser(l2a.getType(false), l2a.getSubType(), l2a));
        regs.add(context.registerExtendedCommunitySerializer(Layer2AttributesExtendedCommunityCase.class, l2a));
    }
}
