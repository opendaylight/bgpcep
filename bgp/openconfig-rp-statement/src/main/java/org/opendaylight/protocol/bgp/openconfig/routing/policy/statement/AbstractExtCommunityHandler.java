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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ExtCommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractExtCommunityHandler {
    private static final InstanceIdentifier<ExtCommunitySets> EXT_COMMUNITY_SETS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .child(ExtCommunitySets.class);
    private final DataBroker databroker;
    protected final LoadingCache<String, List<ExtendedCommunities>> extCommunitySets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<ExtendedCommunities>>() {
                @Override
                public List<ExtendedCommunities> load(final String key)
                        throws ExecutionException, InterruptedException {
                    return loadCommunitySet(key);
                }
            });

    public AbstractExtCommunityHandler(final DataBroker databroker) {
        this.databroker = requireNonNull(databroker);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private List<ExtendedCommunities> loadCommunitySet(final String key)
            throws ExecutionException, InterruptedException {
        final ReadTransaction tr = this.databroker.newReadOnlyTransaction();
        final Optional<ExtCommunitySet> result =
                tr.read(LogicalDatastoreType.CONFIGURATION, EXT_COMMUNITY_SETS_IID
                        .child(ExtCommunitySet.class, new ExtCommunitySetKey(key))).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getExtCommunityMember()
                .stream().map(ge -> new ExtendedCommunitiesBuilder().setExtendedCommunity(ge.getExtendedCommunity())
                        .setTransitive(ge.isTransitive()).build()).collect(Collectors.toList());
    }
}
