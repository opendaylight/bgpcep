package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.CommunitySets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySetKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractCommunityHandler {
    private static final InstanceIdentifier<CommunitySets> COMMUNITY_SETS_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .child(CommunitySets.class);
    private final DataBroker databroker;
    protected final LoadingCache<String, List<Communities>> communitySets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<Communities>>() {
                @Override
                public List<Communities> load(final String key) throws ExecutionException, InterruptedException {
                    return loadCommunitySet(key);
                }
            });

    public AbstractCommunityHandler(final DataBroker dataBroker) {
        this.databroker = requireNonNull(dataBroker);
    }

    private List<Communities> loadCommunitySet(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.databroker.newReadOnlyTransaction();
        final Optional<CommunitySet> result =
                tr.read(LogicalDatastoreType.CONFIGURATION, COMMUNITY_SETS_IID
                        .child(CommunitySet.class, new CommunitySetKey(key))).get();


        if (!result.isPresent()) {
            return Collections.emptyList();
        }

        return result.get().getCommunities()
                .stream().map(ge -> new CommunitiesBuilder().setAsNumber(ge.getAsNumber())
                        .setSemantics(ge.getSemantics()).build()).collect(Collectors.toList());
    }
}
