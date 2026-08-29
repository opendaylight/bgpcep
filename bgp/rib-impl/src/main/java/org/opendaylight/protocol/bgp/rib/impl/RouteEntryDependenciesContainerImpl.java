/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class RouteEntryDependenciesContainerImpl implements RouteEntryDependenciesContainer {
    private final @NonNull RIBSupport<?, ?> ribSupport;
    private final @NonNull YangInstanceIdentifier locRibTarget;
    private final @NonNull BGPRibRoutingPolicy routingPolicies;
    private final @NonNull AfiSafiType afiSafiType;
    private final @NonNull BGPPeerTracker peerTracker;

    RouteEntryDependenciesContainerImpl(
            final RIBSupport<?, ?> ribSupport,
            final BGPPeerTracker peerTracker,
            final BGPRibRoutingPolicy routingPolicies,
            final AfiSafiType afiSafiType,
            final YangInstanceIdentifier locRibTarget) {
        this.ribSupport = requireNonNull(ribSupport);
        this.peerTracker = requireNonNull(peerTracker);
        this.afiSafiType = requireNonNull(afiSafiType);
        this.routingPolicies = requireNonNull(routingPolicies);
        this.locRibTarget = requireNonNull(locRibTarget);
    }

    @Override
    public RIBSupport<?, ?> getRIBSupport() {
        return ribSupport;
    }

    @Override
    public TablesKey getLocalTablesKey() {
        return ribSupport.getTablesKey();
    }

    @Override
    public AfiSafiType getAfiSafType() {
        return afiSafiType;
    }

    @Override
    public YangInstanceIdentifier getLocRibTableTarget() {
        return locRibTarget;
    }

    @Override
    public BGPRibRoutingPolicy getRoutingPolicies() {
        return routingPolicies;
    }

    @Override
    public BGPPeerTracker getPeerTracker() {
        return peerTracker;
    }
}
