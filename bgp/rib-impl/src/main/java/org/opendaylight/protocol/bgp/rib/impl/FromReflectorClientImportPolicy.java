/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Invoked on routes which we get from our reflector peers. This is a special-case of
 * FromInternalImportPolicy.
 */
final class FromReflectorClientImportPolicy extends FromInternalImportPolicy {
    FromReflectorClientImportPolicy(final Ipv4Address bgpIdentifier, final ClusterIdentifier clusterIdentifier) {
        super(bgpIdentifier, clusterIdentifier);
    }

    @Override
    ContainerNode effectiveAttributes(final ContainerNode attributes) {
        // TODO: (defensiveness) verify ORIGINATOR_ID (should have been set)

        return super.effectiveAttributes(attributes);
    }
}
