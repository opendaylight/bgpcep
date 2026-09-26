/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2026 PATHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.CommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ExtCommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.defined.sets.NeighborSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.neighbor.set.NeighborSetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.BgpClusterIdSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.bgp.cluster.id.sets.ClusterIdSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.cluster.id.set.ClusterIdSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.cluster.id.set.ClusterIdSetKey;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A service providing access to (parts of) configured {@link DefinedSets}.
 *
 * @since 3.0.0
 */
@Singleton
@Component(service = DefinedSetResolver.class)
public final class DefinedSetResolver {
    private abstract class Loader<K extends ChildOf<?>, V> extends CacheLoader<@NonNull String, V> {
        @Override
        public final V load(final String key) throws ExecutionException, InterruptedException {
            final ListenableFuture<Optional<K>> future;
            try (var tx = dataBroker.newReadOnlyTransaction()) {
                future = tx.read(LogicalDatastoreType.CONFIGURATION, pathOf(key));
            }
            return valueOf(future.get());
        }

        @NonNullByDefault
        abstract DataObjectIdentifier<K> pathOf(String key);

        abstract V valueOf(Optional<K> key);
    }

    private abstract class NullableLoader<K extends ChildOf<?>, V> extends Loader<K, V> {
        @Override
        final V valueOf(final Optional<K> key) {
            return key.isEmpty() ? absentValue() : presentValue(key.orElseThrow());
        }

        abstract V absentValue();

        abstract V presentValue(K key);
    }

    private abstract class ListLoader<K extends ChildOf<?>, V> extends NullableLoader<K, List<V>> {
        @Override
        final List<V> absentValue() {
            return List.of();
        }

        @Override
        final List<V> presentValue(final K key) {
            return List.copyOf(presentValues(key).toList());
        }

        abstract Stream<V> presentValues(K key);
    }

    private final LoadingCache<@NonNull String, List<Communities>> communitySets = CacheBuilder.newBuilder()
        .build(new ListLoader<CommunitySet, Communities>() {
            private static final DataObjectIdentifier<CommunitySets> PREFIX =
                DataObjectIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class)
                    .child(DefinedSets.class)
                    .augmentation(DefinedSets1.class)
                    .child(BgpDefinedSets.class)
                    .child(CommunitySets.class)
                    .build();

            @Override
            DataObjectIdentifier<CommunitySet> pathOf(final String key) {
                return PREFIX.toBuilder().child(CommunitySet.class, new CommunitySetKey(key)).build();
            }

            @Override
            Stream<Communities> presentValues(final CommunitySet key) {
                final var communities = key.getCommunities();
                return communities == null ? Stream.empty() : communities.stream()
                    .map(ge -> new CommunitiesBuilder()
                        .setAsNumber(ge.getAsNumber())
                        .setSemantics(ge.getSemantics())
                        .build());
            }
        });

    private final LoadingCache<@NonNull String, List<ExtendedCommunities>> extCommunitySets = CacheBuilder.newBuilder()
        .build(new ListLoader<ExtCommunitySet, ExtendedCommunities>() {
            private static final DataObjectIdentifier<ExtCommunitySets> PREFIX =
                DataObjectIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class)
                    .child(DefinedSets.class)
                    .augmentation(DefinedSets1.class)
                    .child(BgpDefinedSets.class)
                    .child(ExtCommunitySets.class)
                    .build();

            @Override
            DataObjectIdentifier<ExtCommunitySet> pathOf(final String key) {
                return PREFIX.toBuilder().child(ExtCommunitySet.class, new ExtCommunitySetKey(key)).build();
            }

            @Override
            Stream<ExtendedCommunities> presentValues(final ExtCommunitySet key) {
                final var communities = key.getExtCommunityMember();
                return communities == null ? Stream.empty() : communities.stream()
                    .map(ge -> new ExtendedCommunitiesBuilder()
                        .setExtendedCommunity(ge.getExtendedCommunity())
                        .setTransitive(ge.getTransitive())
                        .build());
            }
        });

    private final LoadingCache<@NonNull String, List<PeerId>> peerSets = CacheBuilder.newBuilder()
        .build(new ListLoader<NeighborSet, PeerId>() {
            private static final DataObjectIdentifier<NeighborSets> PREFIX =
                DataObjectIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class)
                    .child(DefinedSets.class)
                    .child(NeighborSets.class)
                    .build();

            @Override
            DataObjectIdentifier<NeighborSet> pathOf(final String key) {
                return PREFIX.toBuilder().child(NeighborSet.class, new NeighborSetKey(key)).build();
            }

            @Override
            Stream<PeerId> presentValues(final NeighborSet key) {
                final var neighbor = key.getNeighbor();
                return neighbor == null ? Stream.empty() : neighbor.keySet().stream()
                    .map(nei -> RouterIds.createPeerId(nei.getAddress()));
            }
        });

    private final LoadingCache<@NonNull String, Optional<ClusterIdSet>> clusterIdSets = CacheBuilder.newBuilder()
        .build(new Loader<ClusterIdSet, Optional<ClusterIdSet>>() {
            private static final DataObjectIdentifier<ClusterIdSets> PREFIX =
                DataObjectIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class)
                .child(DefinedSets.class)
                .augmentation(DefinedSets1.class)
                .child(BgpDefinedSets.class)
                .augmentation(BgpClusterIdSets.class)
                .child(ClusterIdSets.class)
                .build();

            @Override
            DataObjectIdentifier<ClusterIdSet> pathOf(final String key) {
                return PREFIX.toBuilder().child(ClusterIdSet.class, new ClusterIdSetKey(key)).build();
            }

            @Override
            Optional<ClusterIdSet> valueOf(final Optional<ClusterIdSet> key) {
                return key;
            }
        });

    private final @NonNull DataBroker dataBroker;

    @Inject
    @Activate
    @NonNullByDefault
    public DefinedSetResolver(@Reference final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    // FIXME: @Nullable
    public List<Communities> lookupCommunitySet(final String communitySetRef) {
        return lookupRef(communitySets, communitySetRef);
    }

    // FIXME: @Nullable
    public List<ExtendedCommunities> lookupExtCommunitySet(final String extCommunitySetRef) {
        return lookupRef(extCommunitySets, extCommunitySetRef);
    }

    public @Nullable ClusterIdSet lookupClusterIdSet(final String clusterIdSetRef) {
        return lookupRef(clusterIdSets, clusterIdSetRef).orElse(null);
    }

    // FIXME: @Nullable
    public List<PeerId> lookupNeighborSet(final String neighborSetRef) {
        return lookupRef(peerSets, neighborSetRef);
    }

    private static <V> V lookupRef(final LoadingCache<@NonNull String, V> cache, final String ref) {
        // FIXME: ditch use of StringUtils
        // FIXME: explain what are we doing here, exactly?
        return cache.getUnchecked(StringUtils.substringBetween(ref, "=\"", "\""));
    }
}
