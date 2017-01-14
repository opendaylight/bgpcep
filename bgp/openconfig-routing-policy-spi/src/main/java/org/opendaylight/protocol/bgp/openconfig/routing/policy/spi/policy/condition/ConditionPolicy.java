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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Condition Policy: Check if route matches defined condition
 */
public interface ConditionPolicy {
    /**
     * Check if route matches defined condition (Import Policy)
     *
     * @param policyRIBBaseParameters contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters     contains route basic information
     * @param attributes              attributes
     * @param conditions              configured conditions
     * @return true if all defined condition matches
     */
    boolean matchImportCondition(
        @Nonnull PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull BGPRouteBaseParameters routeBaseParameters,
        ContainerNode attributes,
        Augmentation<Conditions> conditions);

    /**
     * Check if route matches defined condition (Export Policy)
     *
     * @param policyRIBBaseParameters contains RibBaseParameters(AS, ClusterId, OriginatorId)
     * @param exportParameters        route basic export information
     * @param attributes              attributes
     * @param conditions              configured conditions
     * @return true if all defined condition matches
     */
    boolean matchExportCondition(
        @Nonnull PolicyRIBBaseParameters policyRIBBaseParameters,
        @Nonnull BGPRouteBaseExportParameters exportParameters,
        ContainerNode attributes,
        Augmentation<Conditions> conditions);

}
