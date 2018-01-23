/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Invoked on routes which we send to our reflector peers. This is a special-case of
 * FromInternalImportPolicy.
 */
public final class ToReflectorClientExportPolicy extends AbstractReflectingExportPolicy {
    @Override
    public ContainerNode applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final ContainerNode attributes,
            final Augmentation<Actions> actions) {
        final PeerRole fromPeerRole = exportParameters.getFromPeerRole();
        switch (fromPeerRole) {
            case Ebgp:
                // eBGP -> Client iBGP, propagate
                return attributes;
            case Ibgp:
                // Non-Client iBGP -> Client iBGP, reflect
                return reflectedAttributes(attributes, routeEntryInfo);
            case RrClient:
                // Client iBGP -> Client iBGP, reflect
                return reflectedAttributes(attributes, routeEntryInfo);
            case Internal:
                // Internal Client iBGP -> Client iBGP, reflect
                return reflectedFromInternalAttributes(attributes);
            default:
                throw new IllegalArgumentException("Unhandled source role " + fromPeerRole);
        }
    }
}
