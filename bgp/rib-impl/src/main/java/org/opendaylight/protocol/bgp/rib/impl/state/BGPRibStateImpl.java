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
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPathsCounter;
import org.opendaylight.protocol.bgp.rib.impl.state.rib.TotalPrefixesCounter;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class BGPRibStateImpl extends DefaultRibReference implements BGPRibState, BGPRibStateProvider {
    private final BgpId routeId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private final Map<TablesKey, TotalPathsCounter> totalPaths = new HashMap<>();
    @GuardedBy("this")
    private final Map<TablesKey, TotalPrefixesCounter> totalPrefixes = new HashMap<>();
    @GuardedBy("this")
    private boolean active;

    protected BGPRibStateImpl(final DataObjectIdentifier.WithKey<Rib, RibKey> instanceIdentifier,
        final @NonNull BgpId routeId, final @NonNull AsNumber localAs) {
        super(instanceIdentifier);
        this.routeId = requireNonNull(routeId);
        this.localAs = requireNonNull(localAs);
    }

    @Override
    public final synchronized Map<TablesKey, Long> getTablesPrefixesCount() {
        return totalPrefixes.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPrefixesCount()));
    }

    @Override
    public final synchronized Map<TablesKey, Long> getPathsCount() {
        return totalPaths.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPathsCount()));
    }

    @Override
    public final synchronized long getTotalPathsCount() {
        return totalPaths.values().stream().mapToLong(TotalPathsCounter::getPathsCount).sum();
    }

    @Override
    public final synchronized long getTotalPrefixesCount() {
        return totalPrefixes.values().stream().mapToLong(TotalPrefixesCounter::getPrefixesCount).sum();
    }

    @Override
    public final synchronized long getPathCount(final TablesKey tablesKey) {
        return totalPaths.get(tablesKey).getPathsCount();
    }

    @Override
    public final synchronized long getPrefixesCount(final TablesKey tablesKey) {
        return totalPrefixes.get(tablesKey).getPrefixesCount();
    }

    @Override
    public final AsNumber getAs() {
        return localAs;
    }

    @Override
    public final BgpId getRouteId() {
        return routeId;
    }

    protected final synchronized void registerTotalPathCounter(final @NonNull TablesKey key,
            final @NonNull TotalPathsCounter totalPathsCounter) {
        totalPaths.put(key, totalPathsCounter);
    }

    protected final synchronized void registerTotalPrefixesCounter(final @NonNull TablesKey key,
            final @NonNull TotalPrefixesCounter totalPrefixesCounter) {
        totalPrefixes.put(key, totalPrefixesCounter);
    }

    @Override
    public final synchronized boolean isActive() {
        return active;
    }

    protected final synchronized void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public final BGPRibState getRIBState() {
        return this;
    }
}
