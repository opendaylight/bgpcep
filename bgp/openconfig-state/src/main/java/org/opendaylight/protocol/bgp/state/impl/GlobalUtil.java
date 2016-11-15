/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

public class GlobalUtil {
    private GlobalUtil() {
        throw new UnsupportedOperationException();
    }

    public static Global buildGlobal(final BGPRIBState bgpStateConsumer,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return  new GlobalBuilder().setState(buildState(bgpStateConsumer))
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(buildAfisSafis(bgpStateConsumer, bgpTableTypeRegistry)).build())
            .build();
    }
    public static List<AfiSafi> buildAfisSafis(final BGPRIBState bgpStateConsumer,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return bgpStateConsumer.getPathsCount().keySet().stream()
            .map(tk -> buildAfiSafi(getAfiSafi(tk, bgpTableTypeRegistry), bgpStateConsumer.getPathCount(tk),
                bgpStateConsumer.getPrefixesCount(tk)))
            .collect(Collectors.toList());
    }

    public static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.State buildState(
        final BGPRIBState bgpStateConsumer) {
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.StateBuilder()
            .setAs(bgpStateConsumer.getAs())
            .setRouterId(bgpStateConsumer.getRouteId())
            .setTotalPaths(bgpStateConsumer.getTotalPathsCount())
            .setTotalPrefixes(bgpStateConsumer.getTotalPrefixesCount())
            .build();
    }

    public static Class<? extends AfiSafiType> getAfiSafi(final TablesKey tablesKey,
        final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
            final Optional<Class<? extends AfiSafiType>> optAfiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
            if(optAfiSafi.isPresent()) {
                return optAfiSafi.get();
            }
        return null;
    }

    public static AfiSafi buildAfiSafi(@Nullable final Class<? extends AfiSafiType> afiSafiType,
        final long totalPathCount, final long totalPrefixesCount) {
        if (afiSafiType == null) {
            return null;
        }
        final State state = new StateBuilder()
            .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
                .setTotalPaths(totalPathCount).setTotalPrefixes(totalPrefixesCount).build()).build();
        return new AfiSafiBuilder().setAfiSafiName(afiSafiType).setState(state).build();
    }
}
