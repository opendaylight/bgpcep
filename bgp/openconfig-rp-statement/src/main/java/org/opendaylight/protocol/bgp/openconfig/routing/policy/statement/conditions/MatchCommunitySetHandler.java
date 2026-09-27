/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetsIndex;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;

/**
 * Match a set of Communities (ALL, ANY, INVERT).
 */
public final class MatchCommunitySetHandler implements BgpConditionsPolicy<MatchCommunitySet, List<Communities>> {
    private final @NonNull DefinedSetsIndex definedSets;

    @NonNullByDefault
    public MatchCommunitySetHandler(final DefinedSetsIndex resolver) {
        definedSets = requireNonNull(resolver);
    }

    @Override
    public List<Communities> getConditionParameter(final Attributes attributes) {
        return attributes.getCommunities();
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final List<Communities> communities,
            final MatchCommunitySet conditions) {
        return matchCondition(communities, conditions);
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final List<Communities> communities,
            final MatchCommunitySet conditions) {
        return matchCondition(communities, conditions);
    }

    private boolean matchCondition( final List<Communities> communities, final MatchCommunitySet conditions) {
        final var communitySet = definedSets.lookupCommunitySet(conditions.getCommunitySet());
        // FIXME: document why isEmpty() has type-independent treatment
        if (communitySet == null || communitySet.isEmpty()) {
            return false;
        }

        // FIXME: push this check down
        final var commAttributeList = communities != null ? communities : List.of();
        return switch (conditions.getMatchSetOptions()) {
            case ALL -> commAttributeList.containsAll(communitySet) && communitySet.containsAll(commAttributeList);
            case ANY -> !Collections.disjoint(commAttributeList, communitySet);
            case INVERT -> Collections.disjoint(commAttributeList, communitySet);
        };
    }
}
