/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;

public interface BestPath {

    /**
     * RouterId.
     *
     * @return the routerId
     */
    RouterId getRouterId();

    /**
     * PeerId.
     *
     * @return the routerId converted to a PeerId
     */
    default @NonNull PeerId getPeerId() {
        return getRouterId().getPeerId();
    }

    /**
     * Attributes.
     *
     * @return the path attributes
     */
    @NonNull Attributes getAttributes();

    /**
     * PathId.
     *
     * @return pathId
     */
    long getPathId();

    /**
     * Return true if this path is depreferenced.
     *
     * @return True if this path is depreferenced.
     */
    boolean isDepreferenced();
}
