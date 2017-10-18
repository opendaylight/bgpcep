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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;

final class PrefixesBuilder {
    private static final Ipv4NextHopCase NEXT_HOP;
    private static long count = 1;

    static {
        NEXT_HOP = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("127.1.1.1")).build()).build();
    }

    private PrefixesBuilder() {
        throw new UnsupportedOperationException();
    }

    static void advertiseIpv4Prefixes(final ChannelOutputLimiter session, final int nPrefixes, final List<String> extCom, final boolean multipartSupport) {
        Ipv4Prefix addressPrefix = new Ipv4Prefix("1.1.1.1/31");
        for (int i = 0; i < nPrefixes; i++) {
            buildAndSend(session, addressPrefix, extCom, multipartSupport);
            if (!multipartSupport) {
                addressPrefix = incrementIpv4Prefix(addressPrefix);
            }
        }
    }

    private static void buildAndSend(final ChannelOutputLimiter session, final Ipv4Prefix addressPrefix, final List<String> extCom,
        final boolean multipartSupport) {
        final Update upd = new UpdateBuilder().setAttributes(createAttributes(extCom, multipartSupport, addressPrefix)).build();
        session.write(upd);
        session.flush();
    }

    private static Attributes createAttributes(final List<String> extCom, final boolean multiPathSupport, final Ipv4Prefix addressPrefix) {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        attBuilder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
        attBuilder.setAsPath(new AsPathBuilder().setSegments(Collections.emptyList()).build());
        attBuilder.setMultiExitDisc(new MultiExitDiscBuilder().setMed((long) 0).build());
        attBuilder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
        attBuilder.setExtendedCommunities(createExtComm(extCom));
        attBuilder.setUnrecognizedAttributes(Collections.emptyList());

        final Ipv4PrefixesBuilder prefixes = new Ipv4PrefixesBuilder().setPrefix(addressPrefix);
        if (multiPathSupport) {
            prefixes.setPathId(new PathId(count));
            count++;
        }
        attBuilder.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(
                new MpReachNlriBuilder().setCNextHop(NEXT_HOP).setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                        .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                                new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder()
                                        .setIpv4Prefixes(Collections.singletonList(prefixes.build())).build()).build()).build()).build()).build());

        return attBuilder.build();
    }

}
