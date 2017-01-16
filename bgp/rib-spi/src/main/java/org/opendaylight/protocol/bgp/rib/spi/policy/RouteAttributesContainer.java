/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Contains Route Attributes, PeerID from sender/receiver, PeerRole from sender/receiver
 */
public interface RouteAttributesContainer extends AdvertizedBesthPathContainer {
    /**
     * route attributes
     *
     * @return ContainerNode
     */
    ContainerNode getAttributes();
}
