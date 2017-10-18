/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Defines the internal hooks invoked when a route is being distributed
 * to a peer.
 */
abstract class AbstractExportPolicy {
    /**
     * Transform outgoing attributes according to policy.
     *
     * @param sourceRole role of the peer which originated the routes
     * @param attributes outgoing attributes
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    abstract ContainerNode effectiveAttributes(PeerRole sourceRole, ContainerNode attributes);
}
