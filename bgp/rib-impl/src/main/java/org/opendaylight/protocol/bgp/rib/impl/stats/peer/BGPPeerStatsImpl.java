/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteTable;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

public final class BGPPeerStatsImpl implements BGPPeerStats {
    private final Set<TablesKey> tablesKeySet;
    private final PerTableTypeRouteCounter adjRibInRouteCounters;
    private final PerTableTypeRouteCounter adjRibOutRouteCounters;
    private final UnsignedInt32Counter sessionEstablishedCounter = new UnsignedInt32Counter("SESSION ESTABLISHED");
    private final BGPNeighborState neighborState;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final Map<TablesKey, Class<? extends AfiSafiType>> afiSafisCache = new ConcurrentHashMap<>();

    public BGPPeerStatsImpl(@Nonnull final String peerName, @Nonnull final Set<TablesKey> tablesKeySet,
        @Nonnull final BGPNeighborState neighborState, @Nonnull final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Preconditions.checkNotNull(peerName);
        this.tablesKeySet = Preconditions.checkNotNull(tablesKeySet);
        this.adjRibInRouteCounters = new PerTableTypeRouteCounter("["+ peerName +"] adj-rib-in route", tablesKeySet);
        this.adjRibOutRouteCounters = new PerTableTypeRouteCounter("["+ peerName +"] adj-rib-out route", tablesKeySet);
        this.neighborState = Preconditions.checkNotNull(neighborState);
        this.tableTypeRegistry = Preconditions.checkNotNull(tableTypeRegistry);
    }

    public PerTableTypeRouteCounter getAdjRibInRouteCounters() {
        return this.adjRibInRouteCounters;
    }

    public PerTableTypeRouteCounter getAdjRibOutRouteCounters() {
        return this.adjRibOutRouteCounters;
    }

    public PerTableTypeRouteCounter getEffectiveRibInRouteCounters() {
        return null;
    }

    private RouteTable createRouteTable(@Nonnull final TablesKey tablesKey) {
        Preconditions.checkNotNull(tablesKey);
        final RouteTable routeTable = new RouteTable();
        // FIXME: setTableType() is DEPRECATED, use setAfi() and setSafi() instead
        routeTable.setTableType("afi=" + tablesKey.getAfi().getSimpleName() + ",safi=" + tablesKey.getSafi().getSimpleName());

        final QName afiQName = BindingReflections.findQName(tablesKey.getAfi()).intern();
        final QName safiQName = BindingReflections.findQName(tablesKey.getSafi()).intern();
        routeTable.setAfi(new IdentityAttributeRef(afiQName.toString()));
        routeTable.setSafi(new IdentityAttributeRef(safiQName.toString()));
        // we want to get default counter in case particular route table is not initialized (e.g. adj-rib-out is not initialized in some cases)
        routeTable.setAdjRibInRoutesCount(this.adjRibInRouteCounters.getCounterOrDefault(tablesKey).getCountAsZeroBasedCounter32());
        routeTable.setAdjRibOutRoutesCount(this.adjRibOutRouteCounters.getCounterOrDefault(tablesKey).getCountAsZeroBasedCounter32());
        final UnsignedInt32Counter effCounter = this.neighborState.getPrefixesInstalledCounter(getAfiSafi(tablesKey));
        if(effCounter != null) {
            routeTable.setEffectiveRibInRoutesCount(effCounter.getCountAsZeroBasedCounter32());
        }
        return routeTable;
    }

    @Override
    public BgpPeerState getBgpPeerState() {
        final BgpPeerState peerState = new BgpPeerState();
        final List<RouteTable> routes = Lists.newArrayList();
        this.tablesKeySet.forEach(tablesKey -> routes.add(createRouteTable(tablesKey)));
        peerState.setRouteTable(routes);
        peerState.setSessionEstablishedCount(this.sessionEstablishedCounter.getCountAsZeroBasedCounter32());
        return peerState;
    }

    @Override
    public UnsignedInt32Counter getSessionEstablishedCounter() {
        return this.sessionEstablishedCounter;
    }

    private Class<? extends AfiSafiType> getAfiSafi(final TablesKey tablesKey) {
        Class<? extends AfiSafiType> afiSafi = this.afiSafisCache.get(tablesKey);
        if (afiSafi == null) {
            afiSafi = OpenConfigMappingUtil.toAfiSafi(
                new BgpTableTypeImpl(tablesKey.getAfi(), tablesKey.getSafi()), this.tableTypeRegistry).get()
                .getAfiSafiName();
            this.afiSafisCache.put(tablesKey, afiSafi);
        }
        return afiSafi;
    }
}
