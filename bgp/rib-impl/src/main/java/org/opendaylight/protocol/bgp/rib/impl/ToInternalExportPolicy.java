/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 *  Invoked on routes which we send to our normal home AS peers.
 */
final class ToInternalExportPolicy extends AbstractReflectingExportPolicy {
    ToInternalExportPolicy(final Ipv4Address originatorId, final ClusterIdentifier clusterId) {
        super(originatorId, clusterId);
    }

    @Override
    ContainerNode effectiveAttributes(final PeerRole sourceRole, final ContainerNode attributes) {
        switch (sourceRole) {
        case Ebgp:
            // eBGP -> Non-Client iBGP, propagate
            return attributes;
        case Ibgp:
            // Non-Client iBGP -> Non-Client iBGP, block
            return null;
        case RrClient:
            // Client iBGP -> Non-Client iBGP, reflect
            return reflectedAttributes(attributes);
        case Internal:
            // Internal iBGP -> Non-Client iBGP, reflect
            return reflectedFromInternalAttributes(attributes);
        default:
            throw new IllegalArgumentException("Unhandled source role " + sourceRole);
        }
    }
}