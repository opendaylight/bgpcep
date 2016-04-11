/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.UseMultiplePathsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.EbgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.IbgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi2;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv4Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv6Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Linkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class OpenConfigUtil {

    public static final InstanceIdentifier<Bgp> BGP_IID = InstanceIdentifier.create(Bgp.class);

    public static final String APPLICATION_PEER_GROUP_NAME = "application-peers";

    private static final Map<BgpTableType, AfiSafi> TABLETYPE_TO_AFISAFI;

    static {
        final Builder<BgpTableType, AfiSafi> b = ImmutableMap.builder();
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build());
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build());
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(IPV4LABELLEDUNICAST.class).build());
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(IPV6LABELLEDUNICAST.class).build());
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(L3VPNIPV4UNICAST.class).build());
        b.put(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(Linkstate.class).build());
        b.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(Ipv4Flow.class).build());
        b.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build());
        b.put(new BgpTableTypeImpl(L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class),
            new AfiSafiBuilder().setAfiSafiName(L2VPNEVPN.class).build());
        TABLETYPE_TO_AFISAFI = b.build();
    }

    private OpenConfigUtil() {
        throw new UnsupportedOperationException();
    }

    public static Optional<AfiSafi> toAfiSafi(final BgpTableType tableType) {
        return Optional.fromNullable(TABLETYPE_TO_AFISAFI.get(tableType));
    }

    public static List<AfiSafi> toAfiSafis(final List<BgpTableType> advertizedTables, final BiFunction<AfiSafi, BgpTableType, AfiSafi> function) {
        final List<AfiSafi> afiSafis = new ArrayList<>(advertizedTables.size());
        for (final BgpTableType tableType : advertizedTables) {
            final Optional<AfiSafi> afiSafiMaybe = toAfiSafi(new BgpTableTypeImpl(tableType.getAfi(), tableType.getSafi()));
            if (afiSafiMaybe.isPresent()) {
                final AfiSafi afiSafi = function.apply(afiSafiMaybe.get(), tableType);
                afiSafis.add(afiSafi);
            }
        }
        return afiSafis;
    }

    public static String getModuleName(final String provider) {
        return provider.substring(provider.lastIndexOf('=') + 2, provider.length() - 2);
    }

    public static AfiSafi toNeigborAfiSafiMultiPath(final AfiSafi afiSafi, final BgpTableType tableType, final Collection<AddressFamilies> addPathCapabilities) {
        final java.util.Optional<AddressFamilies> addPathMayBe = addPathCapabilities.stream().filter(
                af -> af.getAfi() == tableType.getAfi() && af.getSafi() == tableType.getSafi()).findFirst();
        if (addPathMayBe.isPresent()) {
            final AfiSafi2 afiSafi3 = new AfiSafi2Builder().setUseMultiplePaths(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.neighbor.UseMultiplePathsBuilder().setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.neighbor.use.multiple.paths.ConfigBuilder()
                        .setEnabled(true).build()).build()).build();
            return new AfiSafiBuilder(afiSafi).addAugmentation(AfiSafi2.class, afiSafi3).build();
        }
        return afiSafi;
    }

    public static AfiSafi toGlobalAfiSafiMultiPath(final AfiSafi afiSafi, final BgpTableType tableType, final Map<TablesKey, PathSelectionMode> pathSelectionModes) {
        final PathSelectionMode pathSelection = pathSelectionModes.get(new TablesKey(tableType.getAfi(), tableType.getSafi()));
        if (pathSelection == null) {
            return afiSafi;
        }
        final Long maxPaths;
        if (pathSelection instanceof AddPathBestNPathSelection) {
            maxPaths = ((AddPathBestNPathSelection) pathSelection).getNBestPaths();
        } else {
            maxPaths = null;
        }
        final AfiSafi1 afiSafi1 = new AfiSafi1Builder().setUseMultiplePaths(
                new UseMultiplePathsBuilder().setConfig(new ConfigBuilder().setEnabled(true).build())
                    .setEbgp(maxPaths != null ? new EbgpBuilder().setConfig(
                            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.ebgp.ConfigBuilder()
                                .setMaximumPaths(maxPaths)
                                .build()).build() : null)
                    .setIbgp(maxPaths != null ? new IbgpBuilder().setConfig(
                        new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.use.multiple.paths.use.multiple.paths.ibgp.ConfigBuilder()
                            .setMaximumPaths(maxPaths).build()).build() : null).build()).build();
        return new AfiSafiBuilder(afiSafi).addAugmentation(AfiSafi1.class, afiSafi1).build();
    }
}
