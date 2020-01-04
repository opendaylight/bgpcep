/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;

public final class PeerRoleUtil {
    private PeerRoleUtil() {
        // Hidden on purpose
    }

    public static String roleForString(final PeerRole role) {
        switch (role) {
            case Ebgp:
                return "ebgp";
            case Ibgp:
                return "ibgp";
            case RrClient:
                return "rr-client";
            case Internal:
                return "internal";
            default:
                throw new IllegalArgumentException("Unhandled role " + role);
        }
    }
}
