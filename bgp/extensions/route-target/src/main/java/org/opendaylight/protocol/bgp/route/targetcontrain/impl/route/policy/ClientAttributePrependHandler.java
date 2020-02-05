/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.route.policy;

import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.ClientAttributePrepend;

public final class ClientAttributePrependHandler implements BgpActionAugPolicy<ClientAttributePrepend> {
    private static final ClientAttributePrependHandler INSTANCE = new ClientAttributePrependHandler();

    private ClientAttributePrependHandler() {

    }

    public static ClientAttributePrependHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final Attributes attributes,
            final ClientAttributePrepend actions) {
        return attributes;
    }


    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final Attributes attributes,
            final ClientAttributePrepend actions) {
        final List<Route> rtRoutes = exportParameters.getClientRouteTargetContrainCache();
        final Optional<Route> found = rtRoutes.stream()
                .filter(rt -> rt.getRouteKey().equals(exportParameters.getRouteKey()))
                .findFirst();
        if (found.isPresent()) {
            return found.get().getAttributes();
        }
        return attributes;
    }
}