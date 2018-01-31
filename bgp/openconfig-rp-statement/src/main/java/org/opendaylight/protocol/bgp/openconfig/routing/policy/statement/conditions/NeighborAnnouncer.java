/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import static org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchRoleSetConditionUtil.matchRoleCondition;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.NeighborAnnouncerCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.match.role.set.condition.MatchRoleSet;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class NeighborAnnouncer implements ConditionPolicy {
    @Override
    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final ContainerNode attributes,
            final Augmentation<Conditions> conditions) {
        Preconditions.checkArgument(conditions instanceof NeighborAnnouncerCondition,
                "The condition augmentation %s is not NeighborAnnouncerCondition type.", conditions);
        final MatchRoleSet neighCond = ((NeighborAnnouncerCondition) conditions).getNeighborAnnouncerCondition()
                .getMatchRoleSet();
        return matchRoleCondition(neighCond, routeEntryImportParameters.getFromPeerRole());
    }

    @Override
    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final ContainerNode attributes,
            final Augmentation<Conditions> conditions) {
        Preconditions.checkArgument(conditions instanceof NeighborAnnouncerCondition,
                "The condition augmentation %s is not NeighborAnnouncerCondition type.", conditions);
        final MatchRoleSet neighCond = ((NeighborAnnouncerCondition) conditions).getNeighborAnnouncerCondition()
                .getMatchRoleSet();
        return matchRoleCondition(neighCond, routeEntryExportParameters.getFromPeerRole());
    }
}