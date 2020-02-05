/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;

/**
 * Common interface for Apply action policy.
 *
 * @param <T> action class
 */
public interface ActionsPolicy<T> {
    /**
     * Applies action to Route Attributes container (Import Policy).
     *
     * @param routeEntryInfo      contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeBaseParameters contains route basic information
     * @param attributes          attributes
     * @param actions             configured Actions
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    @Nullable Attributes applyImportAction(@NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull BGPRouteEntryImportParameters routeBaseParameters, @NonNull Attributes attributes,
            @NonNull T actions);

    /**
     * Applies action to Route Attributes container (Export Policy).
     *
     * @param routeEntryInfo   contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param exportParameters contains route basic export information
     * @param attributes       attributes
     * @param actions          configured Actions
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    @Nullable Attributes applyExportAction(@NonNull RouteEntryBaseAttributes routeEntryInfo,
            @NonNull BGPRouteEntryExportParameters exportParameters, @NonNull Attributes attributes,
            @NonNull T actions);
}
