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
        final ContainerNode ret = AttributeOperations.getInstance(attributes).exportedAttributes(attributes, localAs);

        switch (sourceRole) {
        case Ebgp:
            // eBGP -> eBGP, propagate
            return ret;
        case Ibgp:
            // Non-Client iBGP -> eBGP, propagate
            return ret;
        case RrClient:
            // Client iBGP -> eBGP, propagate
            return ret;
        case Internal:
            // Internal iBGP -> eBGP, propagate
            return ret;
        default:
            throw new IllegalArgumentException("Unhandled source role " + sourceRole);
        }
    }
}