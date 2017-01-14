/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseParameters;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Condition Policy: Check if route matches defined condition
 */
public interface IGPConditionPolicy {
    /**
     * Check if route matches defined IGP condition (Import Policy)
     *
     * @param policyRIBBaseParameters contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters     contains route basic information
     * @param attributes              attributes
     * @param igpConditionPolicy      IGP configured Conditions   @return true if all defined condition matches
     * @return true if all defined condition matches
     */
    boolean matchImport(
        @Nonnull PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull BGPRouteBaseParameters routeBaseParameters,
        ContainerNode attributes,
        Augmentation<IGPConditionPolicy> igpConditionPolicy);

    /**
     * Check if route matches defined IGP condition (Export Policy)
     *
     * @param policyRIBBaseParameters contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param exportParameters        route basic export information
     * @param attributes              attributes
     * @param igpConditionPolicy      IGP configured Conditions
     * @return true if all defined condition matches
     */
    boolean matchExport(
        @Nonnull PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull BGPRouteBaseExportParameters exportParameters,
        ContainerNode attributes,
        Augmentation<IGPConditionPolicy> igpConditionPolicy);
}
