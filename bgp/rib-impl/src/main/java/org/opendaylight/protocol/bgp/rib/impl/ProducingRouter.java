/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

abstract class ProducingRouter extends Router {
    static enum Role {
        /**
         * The router is a Client of this Route Reflector, as defined in
         * RFC 4456.
         */
        CLIENT,
        /**
         * The router is a regular iBGP peering.
         */
        IBGP,
        /**
         * The router is an eBGP peering.
         */
        EBGP,
    }

    /**
     * Return the role of the router in relationship to us.
     *
     * @return The router's role.
     */
    abstract Role getRole();
}
