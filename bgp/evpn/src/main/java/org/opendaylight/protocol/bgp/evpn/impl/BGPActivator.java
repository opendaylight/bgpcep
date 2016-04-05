/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.DefaultGatewayExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.ESILabelExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.ESImpRouteTargetExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.extended.communities.MACMobExtCom;
import org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriActivator;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.extended.communities.extended.community.EsImportRouteExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.extended.communities.extended.community.EsiLabelExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.update.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCase;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    private static final int L2VPN_AFI = 25;
    private static final int EVPN_SAFI = 70;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerSubsequentAddressFamily(EvpnSubsequentAddressFamily.class, EVPN_SAFI));
        regs.add(context.registerAddressFamily(L2vpnAddressFamily.class, L2VPN_AFI));

        NlriActivator.registerNlriParsers(regs);
        registerExtendedCommunities(context, regs);
        ESIActivator.registerEsiTypeParsers(regs);
        return regs;
    }

    private void registerExtendedCommunities(final BGPExtensionProviderContext context, final List<AutoCloseable> regs) {
        final DefaultGatewayExtCom defGEC = new DefaultGatewayExtCom();
        regs.add(context.registerExtendedCommunityParser(defGEC.getType(true), defGEC.getSubType(), defGEC));
        regs.add(context.registerExtendedCommunitySerializer(DefaultGatewayExtendedCommunityCase.class, defGEC));

        final ESILabelExtCom esiLEC = new ESILabelExtCom();
        regs.add(context.registerExtendedCommunityParser(esiLEC.getType(true), esiLEC.getSubType(), esiLEC));
        regs.add(context.registerExtendedCommunitySerializer(EsiLabelExtendedCommunityCase.class, esiLEC));

        final ESImpRouteTargetExtCom esImpEC = new ESImpRouteTargetExtCom();
        regs.add(context.registerExtendedCommunityParser(esImpEC.getType(true), esImpEC.getSubType(), esImpEC));
        regs.add(context.registerExtendedCommunitySerializer(EsImportRouteExtendedCommunityCase.class, esImpEC));

        final MACMobExtCom macEC = new MACMobExtCom();
        regs.add(context.registerExtendedCommunityParser(macEC.getType(true), macEC.getSubType(), macEC));
        regs.add(context.registerExtendedCommunitySerializer(MacMobilityExtendedCommunityCase.class, macEC));
    }


}
