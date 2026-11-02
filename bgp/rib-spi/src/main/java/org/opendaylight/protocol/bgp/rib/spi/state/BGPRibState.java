/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.state;

import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;

/**
 * Representing RIB Operational State information.
 * -PeerGroup States.
 * Total Paths / Total Prefixes counters, representing the paths / prefixes installed on Loc-rib
 */
public interface BGPRibState extends RibReference {
    /**
     * Indicates whether this instance is being actively managed and updated.
     *
     * @return active
     */
    boolean isActive();

    /**
     * {@return per-table RIB counters}
     */
    @NonNullByDefault
    Map<TablesKey, BGPTableState> tableCounters();

    /**
     * Prefixes count per tablesKey Type.
     *
     * @return Prefixes count
     */
    default @NonNull Map<TablesKey, Long> getTablesPrefixesCount() {
        return tableCounters().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().installedPrefixes()));
    }

    /**
     * Mapped Total Paths Count per TableKey.
     *
     * @return Prefixes count
     */
    default @NonNull Map<TablesKey, Long> getPathsCount() {
        return tableCounters().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().installedPaths()));
    }

    /**
     * Total Paths Installed.
     *
     * @return count
     */
    default long getTotalPathsCount() {
        return tableCounters().values().stream().mapToLong(BGPTableState::installedPaths).sum();
    }

    /**
     * Total Prefixes Installed.
     *
     * @return count
     */
    default long getTotalPrefixesCount() {
        return tableCounters().values().stream().mapToLong(BGPTableState::installedPrefixes).sum();
    }

    /**
     * Total Path Installed per specific TableKey.
     *
     * @param tablesKey table key
     * @return count
     */
    default long getPathCount(final TablesKey tablesKey) {
        final var counters = tableCounters().get(tablesKey);
        return counters != null ? counters.installedPaths() : 0;
    }

    /**
     * Total Prefixes Installed per specific TableKey.
     *
     * @param tablesKey table key
     * @return count
     */
    default long getPrefixesCount(final TablesKey tablesKey) {
        final var counters = tableCounters().get(tablesKey);
        return counters != null ? counters.installedPrefixes() : 0;
    }

    /**
     * AS.
     *
     * @return as
     */
    @NonNull AsNumber getAs();

    /**
     * BGP identifier.
     *
     * @return BGP identifier
     */
    @NonNull BgpId getRouteId();
}
