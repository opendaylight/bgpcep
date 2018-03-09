/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.api;

import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;

public interface BestPath {

    /**
     * RouterId.
     *
     * @return the routerId (UnsignedInteger)
     */
    UnsignedInteger getRouterId();

    /**
     * PeerId.
     *
     * @return the routerId (UnsignedInteger) converted to a PeerId
     */
    @Nonnull
    PeerId getPeerId();

    /**
     * Attributes.
     *
     * @return the path attributes
     */
    @Nonnull
    Attributes getAttributes();

    /**
     * PathId.
     *
     * @return pathId
     */
    long getPathId();
}