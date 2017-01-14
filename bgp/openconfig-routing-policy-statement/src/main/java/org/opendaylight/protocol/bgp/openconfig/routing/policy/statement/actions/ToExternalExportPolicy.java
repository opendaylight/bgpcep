/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Invoked on routes which we send outside of our home AS to an External peer.
 */
public final class ToExternalExportPolicy extends AbstractExportAction {
    @Override
    public ContainerNode applyExportAction(
        final PolicyRIBBaseParameters policyRIBBaseParameters,
        final BGPRouteBaseExportParameters exportParameters,
        final ContainerNode attributes,
        final Augmentation<Actions> actions) {
        final ContainerNode ret = AttributeOperations.getInstance(attributes)
            .exportedAttributes(attributes, policyRIBBaseParameters.getLocalAs());

        final PeerRole fromPeerRole = exportParameters.getFromPeerRole();
        switch (fromPeerRole) {
            case Ebgp:
                // eBGP -> eBGP, propagate
                return ret;
            case Ibgp:
                // Non-Client iBGP -> eBGP, propagate
                return ret;
            case RrClient:
                // Client iBGP -> eBGP, propagate
                return ret;
            case Internal:
                // Internal iBGP -> eBGP, propagate
                return ret;
            default:
                throw new IllegalArgumentException("Unhandled source role " + fromPeerRole);
        }
    }
}