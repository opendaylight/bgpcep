/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
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
    private final UnsignedInt32Counter configuredPeerCounter;
    private final UnsignedInt32Counter connectedPeerCounter;

    public BGPRenderStatsImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId, @Nonnull final AsNumber localAs, @Nullable final ClusterIdentifier clusterId) {
        this.bgpId = Preconditions.checkNotNull(bgpId);
        this.ribId = Preconditions.checkNotNull(ribId);
        this.localAs = localAs;
        this.clusterId = clusterId;
        this.configuredPeerCounter = new UnsignedInt32Counter("Configured Peer of BGP-RIB "+ this.ribId.getValue());
        this.connectedPeerCounter = new UnsignedInt32Counter("Connected Peer of BGP-RIB "+ this.ribId.getValue());
    }

    @Override
    public BgpRenderState getBgpRenderState() {
        final BgpRenderState renderState = new BgpRenderState();
        renderState.setRibId(this.ribId);
        renderState.setBgpRibId(this.bgpId);
        renderState.setClusterId(this.clusterId);
        renderState.setLocalAs(this.localAs);
        renderState.setConfiguredPeerCount(this.configuredPeerCounter.getCountAsZeroBasedCounter32());
        renderState.setConnectedPeerCount(this.connectedPeerCounter.getCountAsZeroBasedCounter32());
        // fill in the the statistic part
        final UnsignedInt32Counter totalRouteCount = new UnsignedInt32Counter("Total Loc-Rib Route Count");
        final List<LocRibRouteTable> locRibRouteTableList = new ArrayList<>();
        this.locRibRouteCounter.getCounters()
            .entrySet()
            .stream()
            .forEach(e -> {
                final LocRibRouteTable table = new LocRibRouteTable();
                final QName afi = BindingReflections.getQName(e.getKey().getAfi()).intern();
                final QName safi = BindingReflections.getQName(e.getKey().getSafi()).intern();
                table.setAfi(new IdentityAttributeRef(afi.toString()));
                table.setSafi(new IdentityAttributeRef(safi.toString()));
                table.setRoutesCount(e.getValue().getCountAsZeroBasedCounter32());

                locRibRouteTableList.add(table);
                totalRouteCount.increaseCount(e.getValue().getCount());
            });
        renderState.setLocRibRouteTable(locRibRouteTableList);
        renderState.setLocRibRoutesCount(totalRouteCount.getCountAsZeroBasedCounter32());
        return renderState;
    }

    @Override
    public PerTableTypeRouteCounter getLocRibRouteCounter() {
        return this.locRibRouteCounter;
    }

    @Override
    public UnsignedInt32Counter getConfiguredPeerCounter() {
        return this.configuredPeerCounter;
    }

    @Override
    public UnsignedInt32Counter getConnectedPeerCounter() {
        return this.connectedPeerCounter;
    }
}
