/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRIBRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class RouteEntryDependenciesContainerImpl implements RouteEntryDependenciesContainer {
    private final RIBSupport ribSupport;
    private final BGPRIBRoutingPolicy routingPolicies;
    private final TablesKey localTablesKey;
    private final YangInstanceIdentifier locRibYiID;

    public RouteEntryDependenciesContainerImpl(final RIBSupport ribSupport, final BGPRIBRoutingPolicy routingPolicies,
        final TablesKey localTablesKey, final YangInstanceIdentifier locRibYiID) {
        this.ribSupport = ribSupport;
        this.routingPolicies = routingPolicies;
        this.localTablesKey = localTablesKey;
        this.locRibYiID = locRibYiID;
    }

    @Override
    public RIBSupport getRibSupport() {
        return this.ribSupport;
    }

    @Override
    public BGPRIBRoutingPolicy getRoutingPolicies() {
        return this.routingPolicies;
    }

    @Override
    public TablesKey getLocalTablesKey() {
        return this.localTablesKey;
    }

    @Override
    public YangInstanceIdentifier getLocRibYiID() {
        return this.locRibYiID;
    }
}
