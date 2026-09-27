/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetResolver;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetsIndex;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchRoleSetCondition;

/**
 * Match a Peer Role (FROM, TO).
 */
public final class MatchRoleSetHandler implements BgpConditionsAugmentationPolicy<MatchRoleSetCondition, Void> {
    private final @NonNull DefinedSetsIndex resolver;

    public MatchRoleSetHandler(final DefinedSetResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    @Override
    public Void getConditionParameter(final Attributes attributes) {
        return null;
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters, final Void attributes,
            final MatchRoleSetCondition conditions) {
        return match(importParameters.getFromPeerRole(), null, conditions);
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters, final Void attributes,
            final MatchRoleSetCondition conditions) {
        return match(exportParameters.getFromPeerRole(), exportParameters.getToPeerRole(), conditions);
    }

    private boolean match(final PeerRole fromPeerRole, final @Nullable PeerRole toPeerRole,
            final MatchRoleSetCondition conditions) {
        final var matchRoleSet = conditions.getMatchRoleSet();
        final var from = matchRoleSet.getFromRole();

        // FIXME: refactor this logic
        Boolean match = null;
        if (from != null) {
            match = checkMatch(from.getRoleSet(), fromPeerRole, from.getMatchSetOptions());
        }
        if (match != null && !match) {
            return false;
        }

        final var to = matchRoleSet.getToRole();
        if (to != null) {
            match = checkMatch(to.getRoleSet(), toPeerRole, to.getMatchSetOptions());
        }

        return match;
    }

    private boolean checkMatch(final String roleSetName, final PeerRole role,
            final MatchSetOptionsRestrictedType matchSetOptions) {
        final var roles = resolver.lookupRoleSets(roleSetName);
        return switch (matchSetOptions) {
            case ANY -> roles.contains(role);
            case INVERT -> !roles.contains(role);
        };
    }
}
