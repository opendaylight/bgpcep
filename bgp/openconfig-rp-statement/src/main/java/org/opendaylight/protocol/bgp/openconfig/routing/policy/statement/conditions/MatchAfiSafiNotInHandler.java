/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import java.util.Set;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchAfiSafiNotInCondition;

/**
 * Match NLRI does not pertain to specific AFI/SAFI.
 */
public final class MatchAfiSafiNotInHandler implements
        BgpConditionsAugmentationPolicy<MatchAfiSafiNotInCondition, Void> {

    private static final MatchAfiSafiNotInHandler INSTANCE = new MatchAfiSafiNotInHandler();

    private MatchAfiSafiNotInHandler() {

    }

    public static MatchAfiSafiNotInHandler getInstance() {
        return INSTANCE;
    }

    private static boolean matchAfiSafi(
            final Class<? extends AfiSafiType> afiSafi,
            final Set<Class<? extends AfiSafiType>> afiSafiNotIn) {
        return !afiSafiNotIn.contains(afiSafi);
    }

    @Override
    public boolean matchImportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Void attributes,
            final MatchAfiSafiNotInCondition conditions) {
        return matchAfiSafi(afiSafi, conditions.getAfiSafiNotIn());
    }

    @Override
    public boolean matchExportCondition(
            final Class<? extends AfiSafiType> afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Void attributes,
            final MatchAfiSafiNotInCondition conditions) {
        return matchAfiSafi(afiSafi, conditions.getAfiSafiNotIn());
    }

    @Override
    public Void getConditionParameter(final Attributes attributes) {
        return null;
    }
}
