/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Action Policy to be applied to Route Attributes.
 */
public interface ActionPolicy {
    /**
     * Applies action to Route Attributes container (Import Policy).
     *
     * @param routeEntryInfo      contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters contains route basic information
     * @param attributes          attributes
     * @param actions             configured Actions
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    ContainerNode applyImportAction(
            @Nonnull RouteEntryBaseAttributes routeEntryInfo,
            @Nonnull BGPRouteEntryImportParameters routeBaseParameters,
            ContainerNode attributes,
            Augmentation<Actions> actions);

    /**
     * Applies action to Route Attributes container (Export Policy).
     *
     * @param routeEntryInfo   contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param exportParameters contains route basic export information
     * @param attributes       attributes
     * @param actions          configured Actions
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    ContainerNode applyExportAction(
            @Nonnull RouteEntryBaseAttributes routeEntryInfo,
            @Nonnull BGPRouteEntryExportParameters exportParameters,
            ContainerNode attributes,
            Augmentation<Actions> actions);
}
