/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.state;

import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
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
     * Prefixes count per tablesKey Type.
     *
     * @return Prefixes count
     */
    @NonNull Map<TablesKey, Long> getTablesPrefixesCount();

    /**
     * Mapped Total Paths Count per TableKey.
     *
     * @return Prefixes count
     */
    @NonNull Map<TablesKey, Long> getPathsCount();

    /**
     * Total Paths Installed.
     *
     * @return count
     */
    long getTotalPathsCount();

    /**
     * Total Prefixes Installed.
     *
     * @return count
     */
    long getTotalPrefixesCount();

    /**
     * Total Path Installed per specific TableKey.
     *
     * @param tablesKey table key
     * @return count
     */
    long getPathCount(TablesKey tablesKey);

    /**
     * Total Prefixes Installed per specific TableKey.
     *
     * @param tablesKey table key
     * @return count
     */
    long getPrefixesCount(TablesKey tablesKey);

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

