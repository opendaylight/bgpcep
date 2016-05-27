/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RouteTable;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.BGPPeerStats;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author Kevin Wang
 */
public final class BGPPeerStatsImpl implements BGPPeerStats {

    private final Set<TablesKey> tablesKeySet;
    @GuardedBy("this")
    private long sessionEstablishedCounter = 0L;

    public BGPPeerStatsImpl(@Nonnull final Set<TablesKey> tablesKeySet) {
        this.tablesKeySet = Preconditions.checkNotNull(tablesKeySet);
    }

    @Override
    public BgpPeerState getBgpPeerState() {
        final BgpPeerState peerState = new BgpPeerState();
        final List<RouteTable> routes = Lists.newArrayList();
        for (final TablesKey tablesKey : this.tablesKeySet) {
            final RouteTable routeTable = new RouteTable();
            // FIXME: DEPRECATED, use setAfi() and setSafi() instead
            routeTable.setTableType("afi=" + tablesKey.getAfi().getSimpleName() + ",safi=" + tablesKey.getSafi().getSimpleName());
            final QName afiQName = BindingReflections.findQName(tablesKey.getAfi()).intern();
            final QName safiQName = BindingReflections.findQName(tablesKey.getSafi()).intern();
            routeTable.setAfi(new IdentityAttributeRef(afiQName.toString()));
            routeTable.setSafi(new IdentityAttributeRef(safiQName.toString()));
            routes.add(routeTable);
        }
        peerState.setRouteTable(routes);
        peerState.setSessionEstablishedCount(new ZeroBasedCounter32(this.sessionEstablishedCounter));
        return peerState;
    }

    @Override
    public void increaseSessionEstablishedCounter() {
        this.sessionEstablishedCounter++;
    }
}
