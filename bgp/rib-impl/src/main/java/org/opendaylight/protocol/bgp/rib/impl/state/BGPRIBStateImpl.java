/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerGroupState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BGPRIBStateImpl extends DefaultRibReference implements BGPRIBState {
    private static final Logger LOG = LoggerFactory.getLogger(BGPRIBStateImpl.class);

    private final BgpId routeId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private final Map<TablesKey, LongAdder> totalPaths = new HashMap<>();
    @GuardedBy("this")
    private final Map<TablesKey, LongAdder> totalPrefixes = new HashMap<>();
    @GuardedBy("this")
    private final Map<String, BGPPeerGroupState> peerGroups = new HashMap<>();

    protected BGPRIBStateImpl(final KeyedInstanceIdentifier<Rib, RibKey> instanceIdentifier,
        @Nonnull final BgpId routeId, @Nonnull final AsNumber localAs) {
        super(instanceIdentifier);
        this.routeId = Preconditions.checkNotNull(routeId);
        this.localAs = Preconditions.checkNotNull(localAs);
    }

    @Override
    public final synchronized Map<TablesKey, Long> getPrefixesCount() {
        return mapCount(this.totalPrefixes);
    }

    @Override
    public final synchronized Map<TablesKey, Long> getPathsCount() {
        return mapCount(this.totalPaths);
    }

    @Override
    public final synchronized long getTotalPathsCount() {
        return this.totalPaths.values().stream().mapToLong(LongAdder::longValue).sum();
    }

    @Override
    public final synchronized long getTotalPrefixesCount() {
        return this.totalPrefixes.values().stream().mapToLong(LongAdder::longValue).sum();
    }

    @Override
    public final synchronized long getPathCount(TablesKey tablesKey) {
        return this.totalPaths.get(tablesKey).longValue();
    }

    @Override
    public final synchronized long getPrefixesCount(TablesKey tablesKey) {
        return this.totalPrefixes.get(tablesKey).longValue();
    }

    @Override
    public final synchronized AsNumber getAs() {
        return this.localAs;
    }

    @Override
    public final synchronized BgpId getRouteId() {
        return this.routeId;
    }

    @Override
    public final List<BGPPeerGroupState> getPeerGroupsState() {
        return ImmutableList.copyOf(this.peerGroups.values());
    }

    private static Map<TablesKey, Long> mapCount(final Map<TablesKey, LongAdder> collection) {
        return collection.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            (entry) -> entry.getValue().longValue()));
    }

    protected final synchronized void registerTotalPathCounter(@Nonnull final TablesKey key,
        @Nonnull final LongAdder totalPathsCounter) {
        this.totalPaths.put(key, totalPathsCounter);
    }

    protected final synchronized void registerTotalPrefixesCounter(@Nonnull final TablesKey key,
        @Nonnull final LongAdder totalPrefixesCounter) {
        this.totalPrefixes.put(key, totalPrefixesCounter);
    }

    protected final synchronized AbstractRegistration registerNeighbor(final BGPPeerState neighborState,
        final String groupId) {
        BGPPeerGroupState group = this.peerGroups.get(groupId);
        if (group == null) {
            LOG.info("Group State registered {}", neighborState.getNeighborAddress());
            group = new BGPPeerGroupStateImpl(groupId);
            this.peerGroups.put(groupId, group);
        }
        final AbstractRegistration registry = group.registerNeighbor(neighborState);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BGPRIBStateImpl.this) {
                    registry.close();
                    if (BGPRIBStateImpl.this.peerGroups.get(groupId).isEmpty()) {
                        LOG.info("Group State unregistered {}", groupId);
                        BGPRIBStateImpl.this.peerGroups.remove(groupId);
                    }
                }
            }
        };
    }
}
