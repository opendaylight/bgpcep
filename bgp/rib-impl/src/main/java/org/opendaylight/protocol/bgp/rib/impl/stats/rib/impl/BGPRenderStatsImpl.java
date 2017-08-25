/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.impl.CountersUtil.toZeroBasedCounter32;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocRibRouteTable;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

public final class BGPRenderStatsImpl implements BGPRenderStats {
    private final BgpId bgpId;
    private final RibId ribId;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;
    private final LongAdder configuredPeerCounter = new LongAdder();
    private final LongAdder connectedPeerCounter = new LongAdder();
    private final BGPRIBState globalState;
    private final Set<TablesKey> tablesKeys;

    public BGPRenderStatsImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId, @Nonnull final AsNumber localAs,
        @Nullable final ClusterIdentifier clusterId, @Nonnull final BGPRIBState globalState,
        @Nonnull final Set<TablesKey> tablesKeys) {
        this.bgpId = requireNonNull(bgpId);
        this.ribId = requireNonNull(ribId);
        this.globalState = requireNonNull(globalState);
        this.tablesKeys = requireNonNull(tablesKeys);
        this.localAs = localAs;
        this.clusterId = clusterId;
    }

    @Override
    public BgpRenderState getBgpRenderState() {
        final BgpRenderState renderState = new BgpRenderState();
        renderState.setRibId(this.ribId);
        renderState.setBgpRibId(this.bgpId);
        renderState.setClusterId(this.clusterId);
        renderState.setLocalAs(this.localAs);
        renderState.setConfiguredPeerCount(toZeroBasedCounter32(this.configuredPeerCounter));
        renderState.setConnectedPeerCount(toZeroBasedCounter32(this.connectedPeerCounter));
        // fill in the the statistic part
        final LongAdder totalRouteCount = new LongAdder();
        final List<LocRibRouteTable> locRibRouteTableList = new ArrayList<>();
        this.tablesKeys.forEach(e -> generateCounters(e, locRibRouteTableList, totalRouteCount));
        renderState.setLocRibRouteTable(locRibRouteTableList);
        renderState.setLocRibRoutesCount(toZeroBasedCounter32(totalRouteCount));
        return renderState;
    }

    private void generateCounters(final TablesKey tablesKey, final List<LocRibRouteTable> locRibRouteTableList,
        final LongAdder totalRouteCount) {
        final LocRibRouteTable table = new LocRibRouteTable();
        final QName afi = BindingReflections.getQName(tablesKey.getAfi()).intern();
        final QName safi = BindingReflections.getQName(tablesKey.getSafi()).intern();
        table.setAfi(new IdentityAttributeRef(afi.toString()));
        table.setSafi(new IdentityAttributeRef(safi.toString()));
        final long count = this.globalState.getPathCount(tablesKey);
        table.setRoutesCount(new ZeroBasedCounter32(count));
        locRibRouteTableList.add(table);
        totalRouteCount.add(count);

    }

    @Override
    public LongAdder getConfiguredPeerCounter() {
        return this.configuredPeerCounter;
    }

    @Override
    public LongAdder getConnectedPeerCounter() {
        return this.connectedPeerCounter;
    }
}
