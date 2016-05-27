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
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteTable;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public final class BGPPeerStatsImpl implements BGPPeerStats {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerStatsImpl.class);

    private final Set<TablesKey> tablesKeySet;
    private final PerTableTypeRouteCounter adjRibInRouteCounters;
    private final PerTableTypeRouteCounter adjRibOutRouteCounters;
    private final PerTableTypeRouteCounter effectiveRibInRouteCounters;
    private final UnsignedInt32Counter sessionEstablishedCounter = new UnsignedInt32Counter("SESSION ESTABLISHED");
    private final String peerName;

    public BGPPeerStatsImpl(@Nonnull final String peerName, @Nonnull final Set<TablesKey> tablesKeySet) {
        this.peerName = Preconditions.checkNotNull(peerName);
        this.tablesKeySet = Preconditions.checkNotNull(tablesKeySet);
        this.adjRibInRouteCounters = new PerTableTypeRouteCounter("["+ this.peerName +"] adj-rib-in route", tablesKeySet);
        this.adjRibOutRouteCounters = new PerTableTypeRouteCounter("["+ this.peerName +"] adj-rib-out route", tablesKeySet);
        this.effectiveRibInRouteCounters = new PerTableTypeRouteCounter("["+ this.peerName +"] effective-rib-in route", tablesKeySet);
    }

    public PerTableTypeRouteCounter getAdjRibInRouteCounters() {
        return adjRibInRouteCounters;
    }

    public PerTableTypeRouteCounter getAdjRibOutRouteCounters() {
        return adjRibOutRouteCounters;
    }

    public PerTableTypeRouteCounter getEffectiveRibInRouteCounters() {
        return effectiveRibInRouteCounters;
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
        routeTable.setAdjRibInRoutesCount(adjRibInRouteCounters.getCounterOrDefault(tablesKey).getCountAsZeroBasedCounter32());
        routeTable.setAdjRibOutRoutesCount(adjRibOutRouteCounters.getCounterOrDefault(tablesKey).getCountAsZeroBasedCounter32());
        routeTable.setEffectiveRibInRoutesCount(effectiveRibInRouteCounters.getCounterOrDefault(tablesKey).getCountAsZeroBasedCounter32());

        return routeTable;
    }

    @Override
    public BgpPeerState getBgpPeerState() {
        final BgpPeerState peerState = new BgpPeerState();
        final List<RouteTable> routes = Lists.newArrayList();
        this.tablesKeySet.stream().forEach(tablesKey -> routes.add(createRouteTable(tablesKey)));
        peerState.setRouteTable(routes);
        peerState.setSessionEstablishedCount(this.sessionEstablishedCounter.getCountAsZeroBasedCounter32());
        return peerState;
    }

    @Override
    public UnsignedInt32Counter getSessionEstablishedCounter() {
        return this.sessionEstablishedCounter;
    }
}
