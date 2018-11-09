/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.rib.impl.config.GracefulRestartUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

final class PeerUtil {

    private PeerUtil() {
        throw new UnsupportedOperationException();
    }

    static MpReachNlri createMpReachNlri(final IpAddress nextHop, final long pathId, final List<IpPrefix> prefixes) {
        final Class<? extends AddressFamily> afi;
        final CNextHop cNextHop;
        final DestinationType destinationType;
        if (nextHop.getIpv4Address() != null) {
            afi = Ipv4AddressFamily.class;
            cNextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                    .setGlobal(nextHop.getIpv4Address())
                    .build()).build();
            destinationType = new DestinationIpv4CaseBuilder().setDestinationIpv4(
                    new DestinationIpv4Builder().setIpv4Prefixes(prefixes.stream()
                            .map(prefix -> new Ipv4PrefixesBuilder()
                                    .setPathId(new PathId(pathId))
                                    .setPrefix(new Ipv4Prefix(prefix.getIpv4Prefix())).build())
                            .collect(Collectors.toList()))
                            .build()).build();
        } else {
            afi = Ipv6AddressFamily.class;
            cNextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                    .setGlobal(nextHop.getIpv6Address())
                    .build()).build();
            destinationType = new DestinationIpv6CaseBuilder().setDestinationIpv6(
                    new DestinationIpv6Builder().setIpv6Prefixes(prefixes.stream()
                            .map(prefix -> new Ipv6PrefixesBuilder()
                                    .setPathId(new PathId(pathId))
                                    .setPrefix(new Ipv6Prefix(prefix.getIpv6Prefix())).build())
                            .collect(Collectors.toList()))
                            .build()).build();
        }

        return new MpReachNlriBuilder()
                .setCNextHop(cNextHop)
                .setAfi(afi)
                .setSafi(UnicastSubsequentAddressFamily.class)
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destinationType).build())
                .build();
    }

    static Update createUpdate(final BgpOrigin bgpOrigin,
                               final List<Segments> pathSegments,
                               final long preference,
                               final MpReachNlri mpReach,
                               final MpUnreachNlri mpUnreach,
                               final List<Communities> communities) {
        final Origin origin = new OriginBuilder().setValue(bgpOrigin).build();
        final AsPath asPath = new AsPathBuilder().setSegments(pathSegments).build();
        final LocalPref localPref = new LocalPrefBuilder().setPref(preference).build();
        final AttributesBuilder attributeBuilder = new AttributesBuilder()
                .setOrigin(origin).setAsPath(asPath).setLocalPref(localPref).setCommunities(communities);

        if (mpReach != null) {
            attributeBuilder.addAugmentation(Attributes1.class, new Attributes1Builder()
                    .setMpReachNlri(mpReach)
                    .build());
        }

        if (mpUnreach != null) {
            attributeBuilder.addAugmentation(Attributes2.class, new Attributes2Builder()
                    .setMpUnreachNlri(mpUnreach)
                    .build());
        }

        return new UpdateBuilder()
                .setAttributes(attributeBuilder.build())
                .build();
    }

    static BgpParameters createBgpParameters(final List<TablesKey> advertisedTables,
                                             final List<TablesKey> addPathTables,
                                             final Map<TablesKey, Boolean> gracefulTabes,
                                             final int gracefulTimer,
                                             final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulRestartDTOS) {
        final List<OptionalCapabilities> capabilities = new ArrayList<>();
        advertisedTables.forEach(key -> capabilities.add(createMultiprotocolCapability(key)));
        if (addPathTables != null && !addPathTables.isEmpty()) {
            capabilities.add(createAddPathCapability(addPathTables));
        }
        if (gracefulTabes != null && !gracefulTabes.isEmpty()) {
            capabilities.add(new OptionalCapabilitiesBuilder()
                    .setCParameters(GracefulRestartUtil.getGracefulCapability(gracefulTabes, gracefulTimer, false))
                    .build());
        }
        if (llGracefulRestartDTOS != null && !llGracefulRestartDTOS.isEmpty()) {
            capabilities.add(new OptionalCapabilitiesBuilder()
                    .setCParameters(GracefulRestartUtil.getLlGracefulCapability(llGracefulRestartDTOS))
                    .build());
        }
        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }

    private static OptionalCapabilities createMultiprotocolCapability(final TablesKey key) {
        return new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(
                        CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
                                new MultiprotocolCapabilityBuilder()
                                        .setAfi(key.getAfi())
                                        .setSafi(key.getSafi())
                                        .build()).build()).build()).build();
    }

    private static OptionalCapabilities createAddPathCapability(final List<TablesKey> keys) {
        return new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(CParameters1.class,
                        new CParameters1Builder().setAddPathCapability(
                                new AddPathCapabilityBuilder().setAddressFamilies(keys.stream()
                                        .map(key -> new AddressFamiliesBuilder()
                                                .setAfi(key.getAfi())
                                                .setSafi(key.getSafi())
                                                .setSendReceive(SendReceive.Both)
                                                .build())
                                        .collect(Collectors.toList()))
                                        .build()).build()).build()).build();
    }
}
