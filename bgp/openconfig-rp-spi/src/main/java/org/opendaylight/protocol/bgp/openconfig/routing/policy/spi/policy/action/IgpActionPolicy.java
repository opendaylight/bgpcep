/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Action Policy to be applied to Route Attributes.
 */
public interface IgpActionPolicy {
    /**
     * Applies IGP action to Route Attributes container (Import Policy).
     *
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryImportParameters contains route basic information
     * @param attributes                 attributes
     * @param igpActions                 igp Actions   @return modified Route attributes
     * @return modified Route attributes
     */
    ContainerNode applyImportAction(
            @Nonnull RouteEntryInfo routeEntryInfo,
            @Nonnull BGPRouteEntryImportParameters routeEntryImportParameters,
            @Nonnull ContainerNode attributes,
            @Nonnull Augmentation<IgpActions> igpActions);

    /**
     * Applies IGP action to Route Attributes container (Export Policy).
     *
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryExportParameters contains route basic export information
     * @param attributes                 attributes
     * @param igpActions                 igp Actions
     * @return modified Route attributes
     */
    ContainerNode applyExportAction(
            @Nonnull RouteEntryInfo routeEntryInfo,
            @Nonnull BGPRouteEntryExportParameters routeEntryExportParameters,
            @Nonnull ContainerNode attributes,
            @Nonnull Augmentation<IgpActions> igpActions);
}
