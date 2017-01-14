/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Action Policy to be applied to Route Attributes
 */
public interface IgpActionPolicy {
    /**
     * Applies IGP action to Route Attributes container (Import Policy)
     *
     * @param policyRIBBaseParameters contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters     contains route basic information
     * @param attributes              attributes
     * @param igpActions              igp Actions   @return modified Route attributes
     * @return modified Route attributes
     */
    ContainerNode applyImportAction(
        @Nonnull final PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull final BGPRouteBaseParameters routeBaseParameters,
        final ContainerNode attributes,
        @Nonnull Augmentation<IgpActions> igpActions);

    /**
     * Applies IGP action to Route Attributes container (Export Policy)
     *
     * @param policyRIBBaseParameters   contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param routeBaseExportParameters contains route basic export information
     * @param attributes                attributes
     * @param igpActions                igp Actions
     * @return modified Route attributes
     */
    ContainerNode applyExportAction(
        @Nonnull final PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull final BGPRouteBaseExportParameters routeBaseExportParameters,
        final ContainerNode attributes,
        @Nonnull Augmentation<IgpActions> igpActions);
}
