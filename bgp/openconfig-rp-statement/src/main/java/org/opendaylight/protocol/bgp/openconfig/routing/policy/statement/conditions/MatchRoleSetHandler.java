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
import java.util.Optional;
import java.util.Set;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.BgpRoleSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchRoleSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.bgp.role.sets.RoleSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.match.role.set.condition.grouping.MatchRoleSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.match.role.set.condition.grouping.match.role.set.FromRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.match.role.set.condition.grouping.match.role.set.ToRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.role.set.RoleSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.role.set.RoleSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match a Peer Role (FROM, TO).
 */
public final class MatchRoleSetHandler implements BgpConditionsAugmentationPolicy<MatchRoleSetCondition, Void> {
    private static final InstanceIdentifier<RoleSets> ROLE_SET_IID =
        InstanceIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build()
            .child(DefinedSets.class)
            .augmentation(DefinedSets1.class)
            .child(BgpDefinedSets.class)
            .augmentation(BgpRoleSets.class)
            .child(RoleSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, Set<PeerRole>> roleSets = CacheBuilder.newBuilder()
            .build(new CacheLoader<>() {
                @Override
                public Set<PeerRole> load(final String key) throws ExecutionException, InterruptedException {
                    return loadRoleSets(key);
                }
            });

    public MatchRoleSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private Set<PeerRole> loadRoleSets(final String key) throws ExecutionException, InterruptedException {
        final FluentFuture<Optional<RoleSet>> future;
        try (ReadTransaction tr = dataBroker.newReadOnlyTransaction()) {
            future = tr.read(LogicalDatastoreType.CONFIGURATION,
                    ROLE_SET_IID.child(RoleSet.class, new RoleSetKey(key)));
        }
        return future.get().map(RoleSet::getRole).orElse(Set.of());
    }

    @Override
    public boolean matchImportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters,
            final Void attributes,
            final MatchRoleSetCondition conditions) {
        return matchNeighborRoleSetCondition(importParameters.getFromPeerRole(), null,
                conditions.getMatchRoleSet());
    }

    private boolean matchNeighborRoleSetCondition(final PeerRole fromPeerRole, final PeerRole toPeerRole,
            final MatchRoleSet matchRoleSet) {

        final FromRole from = matchRoleSet.getFromRole();
        Boolean match = null;
        if (from != null) {
            match = checkMatch(from.getRoleSet(), fromPeerRole, from.getMatchSetOptions());
        }
        if (match != null && !match) {
            return false;
        }

        final ToRole to = matchRoleSet.getToRole();
        if (to != null) {
            match = checkMatch(to.getRoleSet(), toPeerRole, to.getMatchSetOptions());
        }

        return match;
    }

    private boolean checkMatch(final String roleSetName, final PeerRole role,
            final MatchSetOptionsRestrictedType matchSetOptions) {
        final Set<PeerRole> roles = roleSets.getUnchecked(StringUtils.substringBetween(roleSetName, "=\"", "\""));

        final boolean found = roles.contains(role);
        if (MatchSetOptionsRestrictedType.ANY.equals(matchSetOptions)) {
            return found;
        }
        //INVERT
        return !found;
    }

    @Override
    public boolean matchExportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final Void attributes,
            final MatchRoleSetCondition conditions) {
        return matchNeighborRoleSetCondition(exportParameters.getFromPeerRole(),
                exportParameters.getToPeerRole(), conditions.getMatchRoleSet());
    }

    @Override
    public Void getConditionParameter(final Attributes attributes) {
        return null;
    }
}
