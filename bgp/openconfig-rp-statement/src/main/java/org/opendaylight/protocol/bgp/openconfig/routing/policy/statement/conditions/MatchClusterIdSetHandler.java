/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetResolver;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchClusterIdSetCondition;

/**
 * Match a set of Cluster Id(ALL, NAY, INVERT).
 */
public final class MatchClusterIdSetHandler
        implements BgpConditionsAugmentationPolicy<MatchClusterIdSetCondition, ClusterId> {
    private final @NonNull DefinedSetResolver resolver;

    public MatchClusterIdSetHandler(final DefinedSetResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    @Override
    public ClusterId getConditionParameter(final Attributes attributes) {
        return attributes.getClusterId();
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters, final ClusterId clusterIdAtt,
            final MatchClusterIdSetCondition conditions) {
        return matchCondition(routeEntryInfo, clusterIdAtt, importParameters.getFromClusterId(), conditions);
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters, final ClusterId clusterIdAtt,
            final MatchClusterIdSetCondition conditions) {
        return matchCondition(routeEntryInfo, clusterIdAtt, exportParameters.getFromClusterId(), conditions);
    }

    private boolean matchCondition(final RouteEntryBaseAttributes routeEntryInfo, final ClusterId clusterId,
            final @Nullable ClusterIdentifier paramClusterId, final MatchClusterIdSetCondition conditions) {
        final var localClusterId = paramClusterId != null ? paramClusterId : routeEntryInfo.getClusterId();
        final var matchClusterIdSetCondition = conditions.getMatchClusterIdSetCondition();
        final var clusterIdSet = resolver.lookupClusterIdSet(matchClusterIdSetCondition.getClusterIdSet());
        if (clusterIdSet == null) {
            return false;
        }

        final var matchOption = matchClusterIdSetCondition.getMatchSetOptions();
        // FIXME: refactor into switch expressions with common methods for better clarity
        if (clusterId == null) {
            return matchOption.equals(MatchSetOptionsType.INVERT);
        }

        final var newList = new ArrayList<ClusterIdentifier>();
        final var setClusterId = clusterIdSet.getClusterId();
        if (setClusterId != null) {
            newList.addAll(setClusterId);
        }
        if (clusterIdSet.getLocal() != null) {
            newList.add(localClusterId);
        }

        final var matchClusterList = clusterId.getCluster();
        if (matchOption.equals(MatchSetOptionsType.ALL)) {
            return matchClusterList.containsAll(newList) && newList.containsAll(matchClusterList);
        }
        final boolean noneInCommon = Collections.disjoint(matchClusterList, newList);
        if (matchOption.equals(MatchSetOptionsType.ANY)) {
            return !noneInCommon;
        }
        if (matchOption.equals(MatchSetOptionsType.INVERT)) {
            return noneInCommon;
        }
        return false;
    }
}
