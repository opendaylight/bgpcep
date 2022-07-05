/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;

/**
 * Condition Policy: Check if route matches defined condition.
 */
public interface ConditionsPolicy<T, N> {
    /**
     * Check if route matches defined condition (Import Policy).
     *
     * @param afiSafiType                Afi Safi Type
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryImportParameters contains route basic information
     * @param attributes                 attributes
     * @param conditions                 configured conditions
     * @return true if all defined condition matches
     */
    boolean matchImportCondition(@NonNull AfiSafiType afiSafiType,
            @NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull BGPRouteEntryImportParameters routeEntryImportParameters,
            @Nullable N attributes, @NonNull T conditions);

    /**
     * Check if route matches defined condition (Export Policy).
     *
     * @param afiSafiType                Afi Safi Type
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryExportParameters route basic export information
     * @param attributes                 attributes
     * @param conditions                 configured conditions
     * @return true if all defined condition matches
     */
    boolean matchExportCondition(@NonNull AfiSafiType afiSafiType,
            @NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull BGPRouteEntryExportParameters routeEntryExportParameters,
            @Nullable N attributes,
            T conditions);

    /**
     * Returns the specific attribute to check if match condition.
     *
     * @param attributes route attributes
     * @return specific attribute
     */
    @Nullable N getConditionParameter(@NonNull Attributes attributes);
}
