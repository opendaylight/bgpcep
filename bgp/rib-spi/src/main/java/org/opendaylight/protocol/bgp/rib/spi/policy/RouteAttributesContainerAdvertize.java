/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * Contains RouteEntry information required to advertize to peer the actual Best Path before once session is stabilised
 */
public interface RouteAttributesContainerAdvertize {
    /**
     * Route key
     *
     * @return Route key
     */
    PathArgument getRouteKey();

    /**
     * peer Id of route announced
     *
     * @return PeerId
     */
    PeerId getToPeerId();

    /**
     * router ID pathArgument
     *
     * @return PathArgument
     */
    PathArgument getRouteIdPA();
}
