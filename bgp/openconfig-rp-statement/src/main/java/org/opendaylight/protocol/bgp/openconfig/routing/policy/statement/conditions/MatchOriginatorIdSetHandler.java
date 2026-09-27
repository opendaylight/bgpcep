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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetResolver;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetsIndex;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchOriginatorIdSetCondition;

/**
 * Match an Originator Id(ANY, INVERT).
 */
public final class MatchOriginatorIdSetHandler
        implements BgpConditionsAugmentationPolicy<MatchOriginatorIdSetCondition, OriginatorId> {
    private final @NonNull DefinedSetsIndex resolver;

    public MatchOriginatorIdSetHandler(final DefinedSetResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    @Override
    public OriginatorId getConditionParameter(final Attributes attributes) {
        return attributes.getOriginatorId();
    }

    @Override
    public boolean matchImportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final OriginatorId originatorId,
            final MatchOriginatorIdSetCondition conditions) {
        return match(routeEntryInfo, originatorId, conditions);
    }

    @Override
    public boolean matchExportCondition(final AfiSafiType afiSafi, final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final OriginatorId originatorId,
            final MatchOriginatorIdSetCondition conditions) {
        return match(routeEntryInfo, originatorId, conditions);
    }

    private boolean match(final RouteEntryBaseAttributes routeEntryInfo, final OriginatorId originatorId,
            final MatchOriginatorIdSetCondition conditions) {
        final var condition = conditions.getMatchOriginatorIdSetCondition();
        final var originatorIdSet = resolver.lookupOriginatorIdSet(condition.getOriginatorIdSet());
        if (originatorIdSet == null) {
            return false;
        }

        // FIXME: refactor this logic
        final var localOriginatorId = routeEntryInfo.getOriginatorId();
        boolean found = false;
        if (originatorId != null) {
            final var remOrigin = originatorId.getOriginator();
            if (originatorIdSet.getLocal() != null && localOriginatorId.equals(remOrigin)) {
                found = true;
            }
            if (!found && originatorIdSet.getOriginatorId() != null) {
                found = originatorIdSet.getOriginatorId().contains(remOrigin);
            }
        }
        final var matchOption = condition.getMatchSetOptions();
        return matchOption.equals(MatchSetOptionsRestrictedType.ANY) && found
                || matchOption.equals(MatchSetOptionsRestrictedType.INVERT) && !found;
    }
}
