/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.SetClusterIdPrepend;

/**
 * Prepend Cluster Id.
 */
public final class SetClusterIdPrependHandler implements BgpActionAugPolicy<SetClusterIdPrepend> {
    public static final @NonNull SetClusterIdPrependHandler INSTANCE = new SetClusterIdPrependHandler();

    private SetClusterIdPrependHandler() {
        // hidden on purpose
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters importParameters, final Attributes attributes,
            final SetClusterIdPrepend bgpActions) {
        return prependClusterId(attributes, importParameters.getFromClusterId() == null ? routeEntryInfo.getClusterId()
            : importParameters.getFromClusterId());
    }

    private static Attributes prependClusterId(final Attributes attributes, final ClusterIdentifier clusterId) {
        final var tmp = new ArrayList<ClusterIdentifier>();
        tmp.add(clusterId);
        final var origClusterId = attributes.getClusterId();
        if (origClusterId != null) {
            final var origCluster = origClusterId.getCluster();
            if (origCluster != null && !origCluster.isEmpty()) {
                tmp.addAll(origCluster);
            }
        }
        return new AttributesBuilder(attributes)
            .setClusterId(new ClusterIdBuilder().setCluster(List.copyOf(tmp)).build())
            .build();
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters, final Attributes attributes,
            final SetClusterIdPrepend bgpActions) {
        final var fromClusterId = exportParameters.getFromClusterId();
        return prependClusterId(attributes, fromClusterId != null ? fromClusterId : routeEntryInfo.getClusterId());
    }
}
