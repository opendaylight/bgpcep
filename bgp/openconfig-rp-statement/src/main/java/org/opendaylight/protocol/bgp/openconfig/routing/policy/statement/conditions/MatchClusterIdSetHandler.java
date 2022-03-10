/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.BgpClusterIdSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchClusterIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.bgp.cluster.id.sets.ClusterIdSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.cluster.id.set.ClusterIdSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.cluster.id.set.ClusterIdSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match a set of Cluster Id(ALL, NAY, INVERT).
 */
public final class MatchClusterIdSetHandler
        implements BgpConditionsAugmentationPolicy<MatchClusterIdSetCondition, ClusterId> {
    private static final InstanceIdentifier<ClusterIdSets> CLUSTERS_ID_SETS_IID =
        InstanceIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build()
            .child(DefinedSets.class)
            .augmentation(DefinedSets1.class)
            .child(BgpDefinedSets.class)
            .augmentation(BgpClusterIdSets.class)
            .child(ClusterIdSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, ClusterIdSet> sets = CacheBuilder.newBuilder()
            .build(new CacheLoader<>() {
                @Override
                public ClusterIdSet load(final String key) throws ExecutionException, InterruptedException {
                    return loadSets(key);
                }
            });

    public MatchClusterIdSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private ClusterIdSet loadSets(final String key) throws ExecutionException, InterruptedException {
        final FluentFuture<Optional<ClusterIdSet>> future;
        try (ReadTransaction tr = dataBroker.newReadOnlyTransaction()) {
            future = tr.read(LogicalDatastoreType.CONFIGURATION,
                    CLUSTERS_ID_SETS_IID.child(ClusterIdSet.class, new ClusterIdSetKey(key)));
        }
        return  future.get().orElse(null);
    }

    @Override
    public boolean matchImportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters,
            final ClusterId clusterIdAtt,
            final MatchClusterIdSetCondition conditions) {
        final ClusterIdentifier clusterIdLocal = importParameters.getFromClusterId() == null
                ? routeEntryInfo.getClusterId() : importParameters.getFromClusterId();
        return matchClusterIdCondition(clusterIdLocal, clusterIdAtt,
                conditions.getMatchClusterIdSetCondition());
    }

    @Override
    public boolean matchExportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final ClusterId clusterIdAtt,
            final MatchClusterIdSetCondition conditions) {
        final ClusterIdentifier clusterIdLocal = exportParameters.getFromClusterId() == null
                ? routeEntryInfo.getClusterId() : exportParameters.getFromClusterId();
        return matchClusterIdCondition(clusterIdLocal, clusterIdAtt,
                conditions.getMatchClusterIdSetCondition());
    }

    @Override
    public ClusterId getConditionParameter(final Attributes attributes) {
        return attributes.getClusterId();
    }

    private boolean matchClusterIdCondition(
            final ClusterIdentifier localClusterId,
            final ClusterId clusterId, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .odl.bgp._default.policy.rev200120.match.cluster.id.set.condition.grouping
                .MatchClusterIdSetCondition matchClusterIdSetCondition) {
        final ClusterIdSet clusterIdSet = sets.getUnchecked(StringUtils
                .substringBetween(matchClusterIdSetCondition.getClusterIdSet(), "=\"", "\""));

        if (clusterIdSet == null) {
            return false;
        }
        final MatchSetOptionsType matchOption = matchClusterIdSetCondition.getMatchSetOptions();

        if (clusterId != null) {
            List<ClusterIdentifier> newList = new ArrayList<>();
            if (clusterIdSet.getClusterId() != null) {
                newList.addAll(clusterIdSet.getClusterId());
            }
            if (clusterIdSet.getLocal() != null) {
                newList.add(localClusterId);
            }

            final List<ClusterIdentifier> matchClusterList = clusterId.getCluster();
            if (matchOption.equals(MatchSetOptionsType.ALL)) {
                return matchClusterList.containsAll(newList) && newList.containsAll(matchClusterList);
            }
            final boolean noneInCommon = Collections.disjoint(matchClusterList, newList);
            if (matchOption.equals(MatchSetOptionsType.ANY)) {
                return !noneInCommon;
            } else if (matchOption.equals(MatchSetOptionsType.INVERT)) {
                return noneInCommon;
            }
        } else if (matchOption.equals(MatchSetOptionsType.INVERT)) {
            return true;
        }
        return false;
    }
}
