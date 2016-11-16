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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.LocRibRouteTable;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.state.spi.state.BGPGlobalState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;

public class BGPRenderStatsImpl implements BGPRenderStats {
    private final BgpId bgpId;
    private final RibId ribId;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;
    private final UnsignedInt32Counter configuredPeerCounter;
    private final UnsignedInt32Counter connectedPeerCounter;
    private final BGPGlobalState globalState;
    private final Set<TablesKey> tablesKeys;
    private final Map<TablesKey, Class<? extends AfiSafiType>> afiSafisCache = new ConcurrentHashMap<>();
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;

    public BGPRenderStatsImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId, @Nonnull final AsNumber localAs,
        @Nullable final ClusterIdentifier clusterId, @Nonnull final BGPGlobalState globalState,
        @Nonnull final Set<TablesKey> tablesKeys, @Nonnull final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        this.bgpId = Preconditions.checkNotNull(bgpId);
        this.ribId = Preconditions.checkNotNull(ribId);
        this.globalState = Preconditions.checkNotNull(globalState);
        this.tablesKeys = Preconditions.checkNotNull(tablesKeys);
        this.tableTypeRegistry = Preconditions.checkNotNull(tableTypeRegistry);
        this.localAs = localAs;
        this.clusterId = clusterId;
        this.configuredPeerCounter = new UnsignedInt32Counter("Configured Peer of BGP-RIB " + this.ribId.getValue());
        this.connectedPeerCounter = new UnsignedInt32Counter("Connected Peer of BGP-RIB " + this.ribId.getValue());
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
        this.tablesKeys.forEach(e -> generateCounters(e, locRibRouteTableList, totalRouteCount));
        renderState.setLocRibRouteTable(locRibRouteTableList);
        renderState.setLocRibRoutesCount(totalRouteCount.getCountAsZeroBasedCounter32());
        return renderState;
    }

    private void generateCounters(final TablesKey tablesKey, final List<LocRibRouteTable> locRibRouteTableList,
        final UnsignedInt32Counter totalRouteCount) {
        final LocRibRouteTable table = new LocRibRouteTable();
        final QName afi = BindingReflections.getQName(tablesKey.getAfi()).intern();
        final QName safi = BindingReflections.getQName(tablesKey.getSafi()).intern();
        table.setAfi(new IdentityAttributeRef(afi.toString()));
        table.setSafi(new IdentityAttributeRef(safi.toString()));
        final Class<? extends AfiSafiType> afiSafi = getAfiSafi(tablesKey);
        final UnsignedInt32Counter counter = this.globalState.getPrefixesCounter(afiSafi);
        table.setRoutesCount(counter.getCountAsZeroBasedCounter32());
        locRibRouteTableList.add(table);
        totalRouteCount.incrementCount(counter.getCount());

    }

    @Override
    public PerTableTypeRouteCounter getLocRibRouteCounter() {
        return null;
    }

    @Override
    public UnsignedInt32Counter getConfiguredPeerCounter() {
        return this.configuredPeerCounter;
    }

    @Override
    public UnsignedInt32Counter getConnectedPeerCounter() {
        return this.connectedPeerCounter;
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
