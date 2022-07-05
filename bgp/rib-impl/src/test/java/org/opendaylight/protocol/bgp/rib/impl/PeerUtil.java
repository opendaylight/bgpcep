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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

final class PeerUtil {
    private PeerUtil() {
        // Hidden on purpose
    }

    static MpReachNlri createMpReachNlri(final IpAddressNoZone nextHop, final List<IpPrefix> prefixes) {
        final AddressFamily afi;
        final CNextHop cNextHop;
        final DestinationType destinationType;
        if (nextHop.getIpv4AddressNoZone() != null) {
            afi = Ipv4AddressFamily.VALUE;
            cNextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                    .setGlobal(nextHop.getIpv4AddressNoZone())
                    .build()).build();
            destinationType = new DestinationIpv4CaseBuilder().setDestinationIpv4(
                    new DestinationIpv4Builder().setIpv4Prefixes(prefixes.stream()
                            .map(prefix -> new Ipv4PrefixesBuilder()
                                    .setPathId(PathIdUtil.NON_PATH_ID)
                                    .setPrefix(new Ipv4Prefix(prefix.getIpv4Prefix())).build())
                            .collect(Collectors.toList()))
                            .build()).build();
        } else {
            afi = Ipv6AddressFamily.VALUE;
            cNextHop = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                    .setGlobal(nextHop.getIpv6AddressNoZone())
                    .build()).build();
            destinationType = new DestinationIpv6CaseBuilder().setDestinationIpv6(
                    new DestinationIpv6Builder().setIpv6Prefixes(prefixes.stream()
                            .map(prefix -> new Ipv6PrefixesBuilder()
                                    .setPathId(PathIdUtil.NON_PATH_ID)
                                    .setPrefix(new Ipv6Prefix(prefix.getIpv6Prefix())).build())
                            .collect(Collectors.toList()))
                            .build()).build();
        }

        return new MpReachNlriBuilder()
                .setCNextHop(cNextHop)
                .setAfi(afi)
                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                .setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(destinationType).build())
                .build();
    }

    static Update createUpdate(final BgpOrigin bgpOrigin,
                               final List<Segments> pathSegments,
                               // FIXME: consider using Uint32
                               final long preference,
                               final MpReachNlri mpReach,
                               final MpUnreachNlri mpUnreach) {
        final Origin origin = new OriginBuilder().setValue(bgpOrigin).build();
        final AsPath asPath = new AsPathBuilder().setSegments(pathSegments).build();
        final LocalPref localPref = new LocalPrefBuilder().setPref(Uint32.valueOf(preference)).build();
        final AttributesBuilder attributeBuilder = new AttributesBuilder()
                .setOrigin(origin).setAsPath(asPath).setLocalPref(localPref);

        if (mpReach != null) {
            attributeBuilder.addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReach).build());
        }

        if (mpUnreach != null) {
            attributeBuilder.addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mpUnreach).build());
        }

        return new UpdateBuilder()
                .setAttributes(new AttributesBuilder()
                        .setOrigin(origin)
                        .setAsPath(asPath)
                        .setLocalPref(localPref)
                        .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpReach).build())
                        .build()).build();
    }

    static BgpParameters createBgpParameters(final List<TablesKey> advertisedTables,
                                             final List<TablesKey> addPathTables,
                                             final Map<TablesKey, Boolean> gracefulTabes,
                                             final int gracefulTimer) {
        final List<OptionalCapabilities> capabilities = new ArrayList<>();
        advertisedTables.forEach(key -> capabilities.add(createMultiprotocolCapability(key)));
        if (addPathTables != null && !addPathTables.isEmpty()) {
            capabilities.add(createAddPathCapability(addPathTables));
        }
        if (gracefulTabes != null && !gracefulTabes.isEmpty()) {
            capabilities.add(createGracefulRestartCapability(gracefulTabes, gracefulTimer));
        }
        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }

    private static OptionalCapabilities createMultiprotocolCapability(final TablesKey key) {
        return new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                            .setAfi(key.getAfi())
                            .setSafi(key.getSafi())
                            .build())
                        .build())
                    .build())
                .build();
    }

    private static OptionalCapabilities createGracefulRestartCapability(final Map<TablesKey, Boolean> gracefulTables,
                                                                        final int restartTime) {
        return new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                            .setRestartFlags(new GracefulRestartCapability.RestartFlags(false))
                            .setRestartTime(Uint16.valueOf(restartTime))
                            .setTables(gracefulTables.keySet().stream()
                                .map(key -> new TablesBuilder()
                                    .setAfi(key.getAfi())
                                    .setSafi(key.getSafi())
                                    .setAfiFlags(new Tables.AfiFlags(gracefulTables.get(key)))
                                    .build())
                                .collect(Collectors.toUnmodifiableMap(Tables::key, Function.identity())))
                            .build())
                        .build())
                    .build())
                .build();
    }

    private static OptionalCapabilities createAddPathCapability(final List<TablesKey> keys) {
        return new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .addAugmentation(new CParameters1Builder()
                        .setAddPathCapability(new AddPathCapabilityBuilder()
                            .setAddressFamilies(keys.stream()
                                .map(key -> new AddressFamiliesBuilder()
                                    .setAfi(key.getAfi())
                                    .setSafi(key.getSafi())
                                    .setSendReceive(SendReceive.Both)
                                    .build())
                                .collect(Collectors.toList()))
                            .build())
                        .build())
                    .build())
                .build();
    }
}
