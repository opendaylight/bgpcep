/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocRibRouteTable;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * @author Kevin Wang
 */
public class BGPRenderStatsImpl implements BGPRenderStats {
    private final PerTableTypeRouteCounter locRibRouteCounter = new PerTableTypeRouteCounter("loc-rib route");
    private final BgpId bgpId;
    private final RibId ribId;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;

    public BGPRenderStatsImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId, @Nonnull final AsNumber localAs, @Nullable final ClusterIdentifier clusterId) {
        this.bgpId = Preconditions.checkNotNull(bgpId);
        this.ribId = Preconditions.checkNotNull(ribId);
        this.localAs = localAs;
        this.clusterId = clusterId;
    }

    private static LocRibRouteTable addLocRibRouteTable(final Map.Entry<TablesKey, UnsignedInt32Counter> tableCounterEntry) {
        final LocRibRouteTable table = new LocRibRouteTable();
        final QName afi = BindingReflections.getQName(tableCounterEntry.getKey().getAfi()).intern();
        final QName safi = BindingReflections.getQName(tableCounterEntry.getKey().getSafi()).intern();
        table.setAfi(new IdentityAttributeRef(afi.toString()));
        table.setSafi(new IdentityAttributeRef(safi.toString()));
        table.setRoutesCount(tableCounterEntry.getValue().getCountAsZeroBasedCounter32());
        return table;
    }

    @Override
    public BgpRenderState getBgpRenderState() {
        final BgpRenderState renderState = new BgpRenderState();
        renderState.setBgpRibId(bgpId);
        renderState.setClusterId(clusterId);
        renderState.setLocalAs(localAs);
        renderState.setConfiguredPeerCount(new ZeroBasedCounter32(0L));
        renderState.setConnectedPeerCount(new ZeroBasedCounter32(0L));
        // fill in the the statistic part
        long totalRouteCount = 0L;
        final List<LocRibRouteTable> locRibRouteTableList = this.locRibRouteCounter.getCounters()
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .map(BGPRenderStatsImpl::addLocRibRouteTable)
            .collect(Collectors.toList());
        renderState.setLocRibRouteTable(locRibRouteTableList);
        renderState.setLocRibRoutesCount(new ZeroBasedCounter32(totalRouteCount));
        return renderState;
    }

    @Override
    public PerTableTypeRouteCounter getLocRibRouteCounter() {
        return this.locRibRouteCounter;
    }
}
