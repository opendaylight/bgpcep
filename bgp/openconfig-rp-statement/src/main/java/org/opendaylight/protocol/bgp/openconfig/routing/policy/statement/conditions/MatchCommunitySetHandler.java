/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractCommunityHandler;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;

/**
 * Match a set of Communities (ALL, ANY, INVERT).
 */
public final class MatchCommunitySetHandler
        extends AbstractCommunityHandler implements BgpConditionsPolicy<MatchCommunitySet, List<Communities>> {
    public MatchCommunitySetHandler(final DataBroker databroker) {
        super(databroker);
    }

    @Override
    public boolean matchImportCondition(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final List<Communities> communities,
            final MatchCommunitySet conditions) {
        return matchCondition(communities, conditions.getCommunitySet(), conditions.getMatchSetOptions());
    }

    @Override
    public boolean matchExportCondition(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final List<Communities> communities,
            final MatchCommunitySet conditions) {
        return matchCondition(communities, conditions.getCommunitySet(), conditions.getMatchSetOptions());
    }

    @Override
    public List<Communities> getConditionParameter(final Attributes attributes) {
        return attributes.getCommunities();
    }

    private boolean matchCondition(
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path
                    .attributes.attributes.Communities> communities,
            final String communitySetName,
            final MatchSetOptionsType matchSetOptions) {

        final String setKey = StringUtils
                .substringBetween(communitySetName, "=\"", "\"");
        final List<Communities> communityFilter = communitySets.getUnchecked(setKey);

        if (communityFilter == null || communityFilter.isEmpty()) {
            return false;
        }

        List<Communities> commAttributeList;
        if (communities == null) {
            commAttributeList = List.of();
        } else {
            commAttributeList = communities;
        }

        if (matchSetOptions.equals(MatchSetOptionsType.ALL)) {
            return commAttributeList.containsAll(communityFilter)
                    && communityFilter.containsAll(commAttributeList);
        }
        final boolean noneInCommon = Collections.disjoint(commAttributeList, communityFilter);

        if (matchSetOptions.equals(MatchSetOptionsType.ANY)) {
            return !noneInCommon;
        }
        //(matchSetOptions.equals(MatchSetOptionsType.INVERT))
        return noneInCommon;
    }
}
