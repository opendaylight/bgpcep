/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.testtool;

import static org.opendaylight.protocol.bgp.testtool.CommunitiesBuilder.createExtComm;
import static org.opendaylight.protocol.util.Ipv4Util.incrementIpv4Prefix;

import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.impl.ChannelOutputLimiter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.intra.as.i.pmsi.a.d.grouping.IntraAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.IntraAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

final class PrefixesBuilder {
    private static final Ipv4NextHopCase NEXT_HOP;

    static {
        NEXT_HOP = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                .setGlobal(new Ipv4Address("127.1.1.1")).build()).build();
    }

    private PrefixesBuilder() {
        throw new UnsupportedOperationException();
    }

    static void advertiseIpv4Prefixes(final ChannelOutputLimiter session, final int nprefixes,
            final List<String> extCom, final boolean multipartSupport) {
        Ipv4Prefix addressPrefix = new Ipv4Prefix("1.1.1.1/31");
        for (int i = 0; i < nprefixes; i++) {
            buildAndSend(session, addressPrefix, extCom, multipartSupport);
            addressPrefix = incrementIpv4Prefix(addressPrefix);
        }
    }

    private static void buildAndSend(final ChannelOutputLimiter session, final Ipv4Prefix addressPrefix,
            final List<String> extCom, final boolean multipartSupport) {
        final Update upd = new UpdateBuilder().setAttributes(createAttributes(extCom, multipartSupport, addressPrefix))
                .build();
        session.write(upd);
        session.flush();
    }

    private static Attributes createAttributes(final List<String> extCom, final boolean multiPathSupport,
            final Ipv4Prefix addressPrefix) {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        attBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
        attBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.emptyList()).build());
        attBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        attBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        attBuilder.setExtendedCommunities(createExtComm(extCom));
        attBuilder.setUnrecognizedAttributes(Collections.emptyList());

        final MvpnDestinationBuilder prefixes = new MvpnDestinationBuilder()
                .setMvpnChoice(new IntraAsIPmsiADCaseBuilder().setIntraAsIPmsiAD(new IntraAsIPmsiADBuilder()
                        .setOrigRouteIp(new IpAddress(new Ipv4Address("10.10.10.10")))
                        .setRouteDistinguisher(new RouteDistinguisher(new RdTwoOctetAs("0:5:3")))
                        .build()).build())
                .setPathId(new PathId(0L));
        attBuilder.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(
                new MpReachNlriBuilder().setCNextHop(NEXT_HOP).setAfi(Ipv4AddressFamily.class)
                        .setSafi(McastVpnSubsequentAddressFamily.class)
                        .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                                new DestinationMvpnIpv4AdvertizedCaseBuilder()
                                        .setDestinationMvpn(new DestinationMvpnBuilder()
                                                .setMvpnDestination(Collections.singletonList(prefixes.build()))
                                                    .build())
                                        .build()).build()).build()).build());

        return attBuilder.build();
    }

}
