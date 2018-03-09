/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public final class RouteEntryDependenciesContainerImpl implements RouteEntryDependenciesContainer {
    private final RIBSupport ribSupport;
    private final TablesKey tablesKey;
    private final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget;
    private final BGPRibRoutingPolicy routingPolicies;

    public RouteEntryDependenciesContainerImpl(
            final RIBSupport ribSupport,
            final BGPRibRoutingPolicy routingPolicies,
            final TablesKey tablesKey,
            final KeyedInstanceIdentifier<Tables, TablesKey> locRibTarget) {
        this.ribSupport = requireNonNull(ribSupport);
        this.tablesKey = requireNonNull(tablesKey);
        this.routingPolicies = requireNonNull(routingPolicies);
        this.locRibTarget = requireNonNull(locRibTarget);
    }

    @Override
    public RIBSupport getRibSupport() {
        return this.ribSupport;
    }

    @Override
    public TablesKey getLocalTablesKey() {
        return this.tablesKey;
    }

    @Override
    public KeyedInstanceIdentifier<Tables, TablesKey> getLocRibTableTarget() {
        return this.locRibTarget;
    }

    @Override
    public BGPRibRoutingPolicy getRoutingPolicies() {
        return this.routingPolicies;
    }
}
