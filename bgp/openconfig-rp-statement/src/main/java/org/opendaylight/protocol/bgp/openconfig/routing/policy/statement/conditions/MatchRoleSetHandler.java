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
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.DefinedSets1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.BgpDefinedSets;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.DefinedSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.BgpRoleSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchRoleSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchSetDirectionOptionsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.match.role.set.condition.grouping.MatchRoleSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.role.set.RoleSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.role.set.RoleSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.routing.policy.defined.sets.bgp.defined.sets.RoleSets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Match a Peer Role (FROM, TO).
 */
public final class MatchRoleSetHandler implements BgpConditionsAugmentationPolicy<MatchRoleSetCondition, Void> {
    private static final InstanceIdentifier<RoleSets> ROLE_SET_IID
            = InstanceIdentifier.create(RoutingPolicy.class).child(DefinedSets.class)
            .augmentation(DefinedSets1.class).child(BgpDefinedSets.class)
            .augmentation(BgpRoleSets.class).child(RoleSets.class);
    private final DataBroker dataBroker;
    private final LoadingCache<String, List<PeerRole>> roleSets = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, List<PeerRole>>() {
                @Override
                public List<PeerRole> load(final String key) throws ExecutionException, InterruptedException {
                    return loadRoleSets(key);
                }
            });

    public MatchRoleSetHandler(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    private boolean matchRoleCondition(final MatchRoleSet roleMatch, @Nonnull final PeerRole role) {
        final List<PeerRole> roles = this.roleSets.getUnchecked(StringUtils
                .substringBetween(roleMatch.getRoleSet(), "=\"", "\""));
        final boolean found = roles.contains(role);
        if (roleMatch.getMatchSetOptions().equals(MatchSetOptionsRestrictedType.ANY)) {
            return found;
        }
        //INVERT Case
        return !found;
    }

    private List<PeerRole> loadRoleSets(final String key) throws ExecutionException, InterruptedException {
        final ReadOnlyTransaction tr = this.dataBroker.newReadOnlyTransaction();
        final Optional<RoleSet> result = tr.read(LogicalDatastoreType.CONFIGURATION,
                ROLE_SET_IID.child(RoleSet.class, new RoleSetKey(key))).get();
        if (!result.isPresent()) {
            return Collections.emptyList();
        }
        return result.get().getRole();
    }

    @Override
    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Void attributes,
            final MatchRoleSetCondition conditions) {

        final List<MatchRoleSet> neighCond = conditions.getMatchRoleSet();

        for (final MatchRoleSet roleMatch : neighCond) {
            if (roleMatch.getMatchSetDirectionOptions().equals(MatchSetDirectionOptionsType.TO)) {
                return false;
            } else {
                if (!matchRoleCondition(roleMatch, routeEntryImportParameters.getFromPeerRole())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Void attributes,
            final MatchRoleSetCondition conditions) {
        final List<MatchRoleSet> neighCond = conditions.getMatchRoleSet();

        for (final MatchRoleSet roleMatch : neighCond) {
            PeerRole role;
            if (roleMatch.getMatchSetDirectionOptions().equals(MatchSetDirectionOptionsType.TO)) {
                role = routeEntryExportParameters.getToPeerRole();
            } else {
                role = routeEntryExportParameters.getFromPeerRole();
            }
            if (!matchRoleCondition(roleMatch, role)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public Void getConditionParameter(@Nonnull final Attributes attributes) {
        return null;
    }
}
