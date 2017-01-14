/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.NeighborAnnouncedCondition;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class NeighborAnnounced implements ConditionPolicy {
    @Override
    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final ContainerNode attributes,
            final Augmentation<Conditions> conditions) {
        return false;
    }

    @Override
    public boolean matchExportCondition(
            @Nonnull final RouteEntryBaseAttributes routeEntryInfo,
            @Nonnull final BGPRouteEntryExportParameters routeEntryExportParameters,
            final ContainerNode attributes,
            final Augmentation<Conditions> conditions) {
        Preconditions.checkArgument(conditions instanceof NeighborAnnouncedCondition,
                "The condition augmentation %s is not NeighborAnnouncedCondition type.", conditions);
        final PeerRole matchPeerRole = BGPOpenconfigConditionUtil.toPeerRole(((NeighborAnnouncedCondition) conditions)
                .getNeighborAnnouncedCondition());
        return routeEntryExportParameters.getToPeerRole().equals(matchPeerRole);
    }
}
