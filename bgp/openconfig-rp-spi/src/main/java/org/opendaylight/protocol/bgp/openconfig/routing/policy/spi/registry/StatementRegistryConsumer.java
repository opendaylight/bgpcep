/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryInfo;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;

/**
 * Registry of Statement to be consumed by Export and Import BGPRIBPolicy.
 */
public interface StatementRegistryConsumer {
    /**
     * Apply statement to BGP Route Attributes (Export Policy).
     *
     * @param policyConsumer       OpenconfigPolicy
     * @param routeEntryInfo       contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param baseExportParameters export Parameters
     * @param attributes           route attributes
     * @param statement            Statement containing Conditions/Actions
     * @return modified Route attributes
     */
    @Nonnull
    RouteAttributeContainer applyExportStatement(
            @Nonnull OpenconfigPolicyConsumer policyConsumer,
            @Nonnull RouteEntryInfo routeEntryInfo,
            @Nonnull BGPRouteEntryExportParameters baseExportParameters,
            @Nonnull RouteAttributeContainer attributes,
            @Nonnull Statement statement);

    /**
     * Apply statement to BGP Route Attributes (Import Policy).
     *
     * @param policyConsumer      OpenconfigPolicy
     * @param routeEntryInfo      contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters route base parameters
     * @param attributes          route attributes
     * @param statement           Statement containing Conditions/Actions
     * @return modified Route attributes
     */
    @Nonnull
    RouteAttributeContainer applyImportStatement(
            @Nonnull OpenconfigPolicyConsumer policyConsumer,
            @Nonnull RouteEntryInfo routeEntryInfo,
            @Nonnull BGPRouteEntryImportParameters routeBaseParameters,
            @Nonnull RouteAttributeContainer attributes,
            @Nonnull Statement statement);
}
