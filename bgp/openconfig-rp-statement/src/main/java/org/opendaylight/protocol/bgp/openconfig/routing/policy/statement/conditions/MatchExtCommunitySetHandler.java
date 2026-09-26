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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractExtCommunityHandler;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;

/**
 * Math a set of External Communities (ALL, ANY, INVERT).
 */
public final class MatchExtCommunitySetHandler extends AbstractExtCommunityHandler
        implements BgpConditionsPolicy<MatchExtCommunitySet, List<ExtendedCommunities>> {
    public MatchExtCommunitySetHandler(final DataBroker databroker) {
        super(databroker);
    }

    @Override
    public List<ExtendedCommunities> getConditionParameter(final Attributes attributes) {
        return attributes.getExtendedCommunities();
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final List<ExtendedCommunities> extendedCommunities, final MatchExtCommunitySet conditions) {
        return matchCondition(extendedCommunities, conditions.getExtCommunitySet(), conditions.getMatchSetOptions());
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final List<ExtendedCommunities> extendedCommunities, final MatchExtCommunitySet conditions) {
        return matchCondition(extendedCommunities, conditions.getExtCommunitySet(), conditions.getMatchSetOptions());
    }

    private boolean matchCondition(final List<ExtendedCommunities> extendedCommunities,
            final String matchExtCommunitySetName, final MatchSetOptionsType matchSetOptions) {
        final var setKey = StringUtils.substringBetween(matchExtCommunitySetName, "=\"", "\"");
        final var extCommunityfilter = extCommunitySets.getUnchecked(setKey);
        if (extCommunityfilter == null || extCommunityfilter.isEmpty()) {
            return false;
        }

        // TODO: inline into cases and specialize
        final var extCommList = extendedCommunities != null ? extendedCommunities : List.<ExtendedCommunities>of();
        return switch (matchSetOptions) {
            case ALL -> extCommList.containsAll(extCommunityfilter) && extCommunityfilter.containsAll(extCommList);
            case ANY -> !Collections.disjoint(extCommList, extCommunityfilter);
            case INVERT -> Collections.disjoint(extCommList, extCommunityfilter);
        };
    }
}
