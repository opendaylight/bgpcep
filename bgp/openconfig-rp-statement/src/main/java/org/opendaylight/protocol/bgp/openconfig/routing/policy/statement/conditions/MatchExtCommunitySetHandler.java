/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ExtCommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ext.community.set.ExtCommunityMember;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class MatchExtCommunitySetHandler implements BgpConditionsPolicy<MatchExtCommunitySet> {
    private static final InstanceIdentifier<ExtCommunitySets> EXT_COMMUNITY_SETS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .child(ExtCommunitySets.class);
    private final DataBroker databroker;
    private final LoadingCache<String, List<ExtCommunityMember>> communitySets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<ExtCommunityMember>>() {
                @Override
                public List<ExtCommunityMember> load(final String key)
                        throws ExecutionException, InterruptedException {
                    return loadCommunitySet(key);
                }
            });

    public MatchExtCommunitySetHandler(final DataBroker databroker) {
        this.databroker = requireNonNull(databroker);
    }

    private List<ExtCommunityMember> loadCommunitySet(final String key)
            throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.databroker.newReadOnlyTransaction();
        final Optional<ExtCommunitySet> result =
                tr.read(LogicalDatastoreType.CONFIGURATION, EXT_COMMUNITY_SETS_IID
                        .child(ExtCommunitySet.class, new ExtCommunitySetKey(key))).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getExtCommunityMember();
    }

    private boolean matchCondition(final List<ExtendedCommunities> extendedCommunities,
            final String matchExtCommunitySetName, final MatchSetOptionsType matchSetOptions) {

        final String setKey = StringUtils
                .substringBetween(matchExtCommunitySetName, "=\"", "\"");
        final List<ExtCommunityMember> extCommunityfilter = this.communitySets.getUnchecked(setKey);

        if (extCommunityfilter == null || extCommunityfilter.isEmpty()) {
            return false;
        }

        final List<ExtendedCommunity> extCommAttributeList;
        if (extendedCommunities != null) {
            extCommAttributeList = extendedCommunities.stream()
                    .map(excom -> excom.getExtendedCommunity())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            extCommAttributeList = Collections.emptyList();
        }

        final List<ExtendedCommunity> filter = extCommunityfilter.stream()
                .map(excom -> excom.getExtendedCommunity())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        if (matchSetOptions.equals(MatchSetOptionsType.ALL)) {
            return extCommAttributeList.containsAll(filter)
                    && filter.containsAll(extCommAttributeList);
        }
        final boolean noneInCommon = Collections.disjoint(extCommAttributeList, filter);
        if (matchSetOptions.equals(MatchSetOptionsType.ANY)) {
            return !noneInCommon;
        }
        //(matchSetOptions.equals(MatchSetOptionsType.INVERT))
        return noneInCommon;
    }

    @Override
    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final MatchExtCommunitySet conditions) {
        return matchCondition(attributes.getExtendedCommunities(), conditions.getExtCommunitySet(),
                conditions.getMatchSetOptions());
    }

    @Override
    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final MatchExtCommunitySet conditions) {
        return matchCondition(attributes.getExtendedCommunities(), conditions.getExtCommunitySet(),
                conditions.getMatchSetOptions());
    }
}
