/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;

/**
 * Registry of Statement to be consumed by Export and Import BGPRIBPolicy.
 */
public interface StatementRegistryConsumer {
    /**
     * Apply statement to BGP Route Attributes (Export Policy).
     *
     * @param routeEntryInfo       contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param afiSafi              Afi Safi Type
     * @param baseExportParameters export Parameters
     * @param attributes           route attributes
     * @param statement            Statement containing Conditions/Actions
     * @return modified Route attributes
     */
    @NonNull RouteAttributeContainer applyExportStatement(@NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull AfiSafiType afiSafi, @NonNull BGPRouteEntryExportParameters baseExportParameters,
            @NonNull RouteAttributeContainer attributes, @NonNull Statement statement);

    /**
     * Apply statement to BGP Route Attributes (Import Policy).
     *
     * @param routeEntryInfo      contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param afiSafi             Afi Safi Type
     * @param routeBaseParameters route base parameters
     * @param attributes          route attributes
     * @param statement           Statement containing Conditions/Actions
     * @return modified Route attributes
     */
    @NonNull RouteAttributeContainer applyImportStatement(@NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull AfiSafiType afiSafi, @NonNull BGPRouteEntryImportParameters routeBaseParameters,
            @NonNull RouteAttributeContainer attributes, @NonNull Statement statement);
}
