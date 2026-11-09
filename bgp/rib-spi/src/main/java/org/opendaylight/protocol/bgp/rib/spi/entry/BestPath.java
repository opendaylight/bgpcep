/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

@NonNullByDefault
public interface BestPath {
    /**
     * {@return the {@link RouterId}}
     */
    // FIXME: rename to routerId()
    RouterId getRouterId();

    /**
     * {@return {@link #getRouterId()} converted to a {@link PeerId}}
     */
    // FIXME: rename to peerId()
    default PeerId getPeerId() {
        return getRouterId().getPeerId();
    }

    /**
     * {@return the path attributes}
     */
    // FIXNE: rename to attributes()
    ContainerNode getAttributes();

    /**
     * {@return pathId}
     */
    // FIXME: change to 'int pathIdBits()'
    long getPathId();

    /**
     * {@return {@code true} if this path is depreferenced, {@code false} otherwise}
     */
    // FIXME: rename to depreferenced()
    boolean isDepreferenced();
}
