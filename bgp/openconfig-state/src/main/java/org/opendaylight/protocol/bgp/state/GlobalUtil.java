/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class GlobalUtil {
    private GlobalUtil() {
        // Hidden on purpose
    }

    /**
     * Build Openconfig Global containing RIB group stats from a list of BGP RIB State.
     *
     * @param ribState             containing RIb Operational State
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return Global containing state
     */
    public static @NonNull Global buildGlobal(final BGPRibState ribState,
            final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new GlobalBuilder().setState(buildState(ribState))
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(buildAfisSafis(ribState, bgpTableTypeRegistry)).build())
                .build();
    }

    /**
     * Build per AFI SAFI Openconfig Global State containing RIB group stats from a list of BGP RIB State.
     *
     * @param ribState             containing RIb Operational State
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return List containing per afi/safi operational state
     */
    public static Map<AfiSafiKey, AfiSafi> buildAfisSafis(final BGPRibState ribState,
            final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return ribState.getPathsCount().keySet().stream()
                .map(tk -> buildAfiSafi(ribState, tk, bgpTableTypeRegistry))
                .collect(Collectors.toUnmodifiableMap(AfiSafi::key, Function.identity()));
    }

    /**
     * Build Openconfig Global State.
     *
     * @param ribState containing RIb Operational State
     * @return Openconfig Global State
     */
    public static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
            .@NonNull State buildState(final BGPRibState ribState) {
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder()
                .setAs(ribState.getAs())
                .setRouterId(ribState.getRouteId())
                .setTotalPaths(saturatedUint32(ribState.getTotalPathsCount()))
                .setTotalPrefixes(saturatedUint32(ribState.getTotalPrefixesCount()))
                .build();
    }

    // FIXME: remove this with YANGTOOLS-5.0.7+
    private static Uint32 saturatedUint32(final long value) {
        return value < 4294967295L ? Uint32.valueOf(value) : Uint32.MAX_VALUE;
    }

    /**
     * Build Afi Safi containing State.
     *
     * @param ribState             containing RIb Operational State
     * @param tablesKey            table Key
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return Afi Safi Operational State
     */
    public static @Nullable AfiSafi buildAfiSafi(final BGPRibState ribState, final TablesKey tablesKey,
            final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final AfiSafiType afiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
        return afiSafi == null ? null : new AfiSafiBuilder()
                .setAfiSafiName(afiSafi)
                .setState(new StateBuilder()
                    .addAugmentation(new GlobalAfiSafiStateAugmentationBuilder()
                        .setTotalPaths(saturatedUint32(ribState.getPathCount(tablesKey)))
                        .setTotalPrefixes(saturatedUint32(ribState.getPrefixesCount(tablesKey)))
                        .build())
                    .build())
                .build();
    }
}
