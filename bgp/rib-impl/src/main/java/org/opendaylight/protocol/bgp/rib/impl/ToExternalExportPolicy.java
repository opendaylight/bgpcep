/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Invoked on routes which we send outside of our home AS.
 */
final class ToExternalExportPolicy extends AbstractExportPolicy {
    private final Long localAs;

    ToExternalExportPolicy(final Long localAs) {
        this.localAs = Preconditions.checkNotNull(localAs);
    }

    @Override
    ContainerNode effectiveAttributes(final PeerRole sourceRole, final ContainerNode attributes) {
        // FIXME: filter all non-transitive attributes
        /*
         * FIXME: prepend local AS to add our AS into AS_PATH
         *        http://tools.ietf.org/html/rfc4271#section-5.1.2
         */

        switch (sourceRole) {
        case Ebgp:
            // eBGP -> eBGP, propagate
            return attributes;
        case Ibgp:
            // Non-Client iBGP -> eBGP, propagate
            return attributes;
        case RrClient:
            // Client iBGP -> eBGP, propagate
            return attributes;
        default:
            throw new IllegalArgumentException("Unhandled source role " + sourceRole);
        }
    }
}