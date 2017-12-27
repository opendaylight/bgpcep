/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;


import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPathsCounter;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPrefixesCounter;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class BGPRIBStateImpl extends DefaultRibReference implements BGPRibState, BGPRibStateConsumer {
    private final BgpId routeId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private final Map<TablesKey, TotalPathsCounter> totalPaths = new HashMap<>();
    @GuardedBy("this")
    private final Map<TablesKey, TotalPrefixesCounter> totalPrefixes = new HashMap<>();
    @GuardedBy("this")
    private boolean active;

    protected BGPRIBStateImpl(final KeyedInstanceIdentifier<Rib, RibKey> instanceIdentifier,
        @Nonnull final BgpId routeId, @Nonnull final AsNumber localAs) {
        super(instanceIdentifier);
        this.routeId = requireNonNull(routeId);
        this.localAs = requireNonNull(localAs);
    }

    @Override
    public final synchronized Map<TablesKey, Long> getPrefixesCount() {
        return this.totalPrefixes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            (entry) -> entry.getValue().getPrefixesCount()));
    }

    @Override
    public final synchronized Map<TablesKey, Long> getPathsCount() {
        return this.totalPaths.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            (entry) -> entry.getValue().getPathsCount()));
    }

    @Override
    public final synchronized long getTotalPathsCount() {
        return this.totalPaths.values().stream().mapToLong(TotalPathsCounter::getPathsCount).sum();
    }

    @Override
    public final synchronized long getTotalPrefixesCount() {
        return this.totalPrefixes.values().stream().mapToLong(TotalPrefixesCounter::getPrefixesCount).sum();
    }

    @Override
    public final synchronized long getPathCount(TablesKey tablesKey) {
        return this.totalPaths.get(tablesKey).getPathsCount();
    }

    @Override
    public final synchronized long getPrefixesCount(TablesKey tablesKey) {
        return this.totalPrefixes.get(tablesKey).getPrefixesCount();
    }

    @Override
    public final AsNumber getAs() {
        return this.localAs;
    }

    @Override
    public final BgpId getRouteId() {
        return this.routeId;
    }

    protected final synchronized void registerTotalPathCounter(@Nonnull final TablesKey key,
        @Nonnull final TotalPathsCounter totalPathsCounter) {
        this.totalPaths.put(key, totalPathsCounter);
    }

    protected final synchronized void registerTotalPrefixesCounter(@Nonnull final TablesKey key,
        @Nonnull final TotalPrefixesCounter totalPrefixesCounter) {
        this.totalPrefixes.put(key, totalPrefixesCounter);
    }

    @Override
    public final synchronized boolean isActive() {
        return this.active;
    }

    protected final synchronized void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public final BGPRibState getRIBState() {
        return this;
    }
}
