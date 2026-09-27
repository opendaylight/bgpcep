/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetsIndex;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchBgpNeighborCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.match.bgp.neighbor.grouping.MatchBgpNeighborSet;

/**
 * Match a set of Neighbors(ip address) (ANY, INVERT).
 */
public final class MatchBgpNeighborSetHandler
        implements BgpConditionsAugmentationPolicy<MatchBgpNeighborCondition, Void> {
    private final @NonNull DefinedSetsIndex definedSets;

    public MatchBgpNeighborSetHandler(final DefinedSetsIndex definedSets) {
        this.definedSets = requireNonNull(definedSets);
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters, final Void nonAttributres,
            final MatchBgpNeighborCondition conditions) {
        return matchBgpNeighborSetCondition(importParameters.getFromPeerId(), null,
                conditions.getMatchBgpNeighborSet());
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters, final Void nonAttributres,
            final MatchBgpNeighborCondition conditions) {
        return matchBgpNeighborSetCondition(exportParameters.getFromPeerId(), exportParameters.getToPeerId(),
                conditions.getMatchBgpNeighborSet());
    }

    private boolean matchBgpNeighborSetCondition(final PeerId fromPeerId, final PeerId toPeerId,
            final MatchBgpNeighborSet matchBgpNeighborSet) {
        // FIXME: refactor this code
        final var from = matchBgpNeighborSet.getFromNeighbor();
        Boolean match = null;
        if (from != null) {
            match = checkMatch(from.getNeighborSet(), fromPeerId, from.getMatchSetOptions());
        }

        if (match != null && !match) {
            return false;
        }

        final var to = matchBgpNeighborSet.getToNeighbor();
        if (to != null) {
            match = checkMatch(to.getNeighborSet(), toPeerId, to.getMatchSetOptions());
        }

        return match;
    }

    private boolean checkMatch(final String neighborSetName, final PeerId peerId,
            final MatchSetOptionsRestrictedType matchSetOptions) {
        // FIXME: we are really doing a combined lookup and want to operate on Set.contains()
        final var roles = definedSets.lookupNeighborSet(neighborSetName);
        final boolean found = roles.contains(peerId);
        if (MatchSetOptionsRestrictedType.ANY.equals(matchSetOptions)) {
            return found;
        }
        //INVERT
        return !found;
    }

    @Override
    public Void getConditionParameter(final Attributes attributes) {
        return null;
    }
}
