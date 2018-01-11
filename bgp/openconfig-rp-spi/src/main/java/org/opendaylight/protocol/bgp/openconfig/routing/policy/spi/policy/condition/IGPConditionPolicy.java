/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * IGP Condition Policy: Check if route matches defined condition.
 */
public interface IGPConditionPolicy {
    /**
     * Check if route matches defined IGP condition (Import Policy).
     *
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryImportParameters contains route basic information
     * @param attributes                 attributes
     * @param igpConditionPolicy         IGP configured Conditions   @return true if all defined condition matches
     * @return true if all defined condition matches
     */
    boolean matchImport(
            @Nonnull RouteEntryBaseAttributes routeEntryInfo,
            @Nonnull BGPRouteEntryImportParameters routeEntryImportParameters,
            @Nullable ContainerNode attributes,
            @Nonnull Augmentation<IGPConditionPolicy> igpConditionPolicy);

    /**
     * Check if route matches defined IGP condition (Export Policy).
     *
     * @param routeEntryInfo             contains route Entry Info(AS, ClusterId, OriginatorId)
     * @param routeEntryExportParameters route basic export information
     * @param attributes                 attributes
     * @param igpConditionPolicy         IGP configured Conditions
     * @return true if all defined condition matches
     */
    boolean matchExport(
            @Nonnull RouteEntryBaseAttributes routeEntryInfo,
            @Nonnull BGPRouteEntryExportParameters routeEntryExportParameters,
            @Nullable ContainerNode attributes,
            @Nonnull Augmentation<IGPConditionPolicy> igpConditionPolicy);
}
