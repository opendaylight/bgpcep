/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer;

import static org.opendaylight.protocol.bgp.rib.impl.CountersUtil.toZeroBasedCounter32;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteTable;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

public final class BGPPeerStatsImpl implements BGPPeerStats {
    private final Set<TablesKey> tablesKeySet;
    private final PerTableTypeRouteCounter adjRibInRouteCounters;
    private final PerTableTypeRouteCounter adjRibOutRouteCounters;
    private final LongAdder sessionEstablishedCounter = new LongAdder();
    private final BGPPeerStateImpl neighborState;

    public BGPPeerStatsImpl(@Nonnull final String peerName, @Nonnull final Set<TablesKey> tablesKeySet,
        @Nonnull final BGPPeerStateImpl neighborState) {
        Preconditions.checkNotNull(peerName);
        this.tablesKeySet = Preconditions.checkNotNull(tablesKeySet);
        this.adjRibInRouteCounters = new PerTableTypeRouteCounter(tablesKeySet);
        this.adjRibOutRouteCounters = new PerTableTypeRouteCounter(tablesKeySet);
        this.neighborState = Preconditions.checkNotNull(neighborState);
    }

    public PerTableTypeRouteCounter getAdjRibInRouteCounters() {
        return this.adjRibInRouteCounters;
    }

    public PerTableTypeRouteCounter getAdjRibOutRouteCounters() {
        return this.adjRibOutRouteCounters;
    }

    private RouteTable createRouteTable(@Nonnull final TablesKey tablesKey) {
        Preconditions.checkNotNull(tablesKey);
        final RouteTable routeTable = new RouteTable();

        final QName afiQName = BindingReflections.findQName(tablesKey.getAfi()).intern();
        final QName safiQName = BindingReflections.findQName(tablesKey.getSafi()).intern();
        routeTable.setAfi(new IdentityAttributeRef(afiQName.toString()));
        routeTable.setSafi(new IdentityAttributeRef(safiQName.toString()));
        // we want to get default counter in case particular route table is not initialized (e.g. adj-rib-out is not initialized in some cases)
        routeTable.setAdjRibInRoutesCount(toZeroBasedCounter32(this.adjRibInRouteCounters.getCounterOrDefault(tablesKey)));
        routeTable.setAdjRibOutRoutesCount(toZeroBasedCounter32(this.adjRibOutRouteCounters.getCounterOrDefault(tablesKey)));
        routeTable.setEffectiveRibInRoutesCount(new ZeroBasedCounter32(this.neighborState
            .getPrefixesInstalledCount(tablesKey)));

        return routeTable;
    }

    @Override
    public BgpPeerState getBgpPeerState() {
        final BgpPeerState peerState = new BgpPeerState();
        final List<RouteTable> routes = Lists.newArrayList();
        this.tablesKeySet.forEach(tablesKey -> routes.add(createRouteTable(tablesKey)));
        peerState.setRouteTable(routes);
        peerState.setSessionEstablishedCount(toZeroBasedCounter32(this.sessionEstablishedCounter));
        return peerState;
    }

    @Override
    public LongAdder getSessionEstablishedCounter() {
        return this.sessionEstablishedCounter;
    }
}
