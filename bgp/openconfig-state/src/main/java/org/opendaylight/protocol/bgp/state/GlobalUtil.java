/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

public final class GlobalUtil {
    private GlobalUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Build Openconfig Global containing RIB group stats from a list of BGP RIB State
     *
     * @param ribState containing RIb Operational State
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return
     */
    @Nonnull
    public static Global buildGlobal(final BGPRIBState ribState,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return  new GlobalBuilder().setState(buildState(ribState))
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(buildAfisSafis(ribState, bgpTableTypeRegistry)).build())
            .build();
    }

    /**
     * Build per AFI SAFI Openconfig Global State containing RIB group stats from a list of BGP RIB State
     * @param ribState containing RIb Operational State
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return List containing per afi/safi operational state
     */
    public static List<AfiSafi> buildAfisSafis(final BGPRIBState ribState,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return ribState.getPathsCount().keySet().stream()
            .map(tk -> buildAfiSafi(ribState, tk, bgpTableTypeRegistry))
            .collect(Collectors.toList());
    }

    /**
     * Build Openconfig Global State
     * @param ribState containing RIb Operational State
     * @return Openconfig Global State
     */
    public static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.State buildState(
        final BGPRIBState ribState) {
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder()
            .setAs(ribState.getAs())
            .setRouterId(ribState.getRouteId())
            .setTotalPaths(ribState.getTotalPathsCount())
            .setTotalPrefixes(ribState.getTotalPrefixesCount())
            .build();
    }

    /**
     *
     * @param ribState containing RIb Operational State
     * @param tablesKey table Key
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return Afi Safi Operational State
     */
    public static AfiSafi buildAfiSafi(final BGPRIBState ribState, final TablesKey tablesKey,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Optional<Class<? extends AfiSafiType>> optAfiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
        if (!optAfiSafi.isPresent()) {
            return null;
        }
        final State state = new StateBuilder()
            .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
                .setTotalPaths(ribState.getPathCount(tablesKey)).setTotalPrefixes(ribState.getPrefixesCount(tablesKey))
                .build()).build();
        return new AfiSafiBuilder().setAfiSafiName(optAfiSafi.get()).setState(state).build();
    }
}
