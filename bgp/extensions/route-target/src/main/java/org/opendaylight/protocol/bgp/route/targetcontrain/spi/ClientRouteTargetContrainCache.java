/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;

/**
 * Provide a cache to store client routes advertized, to be used for policy
 * https://tools.ietf.org/html/rfc4684.
 * ii.  When advertising an RT membership NLRI to a non-client peer, if
 * the best path as selected by the path selection procedure
 * described in Section 9.1 of the base BGP specification [4] is a
 * route received from a non-client peer, and if there is an
 * alternative path to the same destination from a client, the
 * attributes of the client path are advertised to the peer"
 */
public interface ClientRouteTargetContrainCache {
    /**
     * Cache Route.
     *
     * @param route target constrain
     */
    void cacheRoute(Route route);

    /**
     * Uncache Route.
     *
     * @param route target constrain
     */
    void uncacheRoute(Route route);
}
