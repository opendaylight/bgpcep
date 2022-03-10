/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
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
import com.google.common.util.concurrent.FluentFuture;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.CommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractCommunityHandler {
    private static final InstanceIdentifier<CommunitySets> COMMUNITY_SETS_IID =
        InstanceIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build()
            .child(DefinedSets.class)
            .augmentation(DefinedSets1.class)
            .child(BgpDefinedSets.class)
            .child(CommunitySets.class);
    protected final LoadingCache<String, List<Communities>> communitySets;

    public AbstractCommunityHandler(final DataBroker dataBroker) {
        requireNonNull(dataBroker);
        communitySets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<Communities>>() {
                @Override
                public List<Communities> load(final String key) throws ExecutionException, InterruptedException {
                    final FluentFuture<Optional<CommunitySet>> future;
                    try (ReadTransaction tr = dataBroker.newReadOnlyTransaction()) {
                        future = tr.read(LogicalDatastoreType.CONFIGURATION,
                            COMMUNITY_SETS_IID.child(CommunitySet.class, new CommunitySetKey(key)));
                    }

                    return future.get().map(set -> set.nonnullCommunities().stream()
                        .map(ge -> new CommunitiesBuilder()
                            .setAsNumber(ge.getAsNumber())
                            .setSemantics(ge.getSemantics())
                            .build())
                        .collect(Collectors.toUnmodifiableList()))
                        .orElse(List.of());
                }
            });
    }
}
