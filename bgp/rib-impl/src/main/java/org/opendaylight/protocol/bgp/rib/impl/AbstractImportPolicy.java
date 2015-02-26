/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Defines the internal hooks invoked when a new route appears.
 */
abstract class AbstractImportPolicy {
    // Invoked on routes which we get from outside of our home AS
    private static final class FromExternalImportPolicy extends AbstractImportPolicy {
        @Override
        ContainerNode effectiveAttributes(final ContainerNode attributes) {
            // FIXME: filter all non-transitive attributes
            // FIXME: but that may end up hurting our informedness
            return attributes;
        }
    }

    // Invoked on routes which we get from our normal home AS peers.
    private static class FromInternalImportPolicy extends AbstractImportPolicy {
        @Override
        ContainerNode effectiveAttributes(final ContainerNode attributes) {
            // FIXME: Implement filtering according to <a ref="http://tools.ietf.org/html/rfc4456#section-8"/>.
            return attributes;
        }
    }

    /**
     * Invoked on routes which we get from our reflector peers. This is a special-case of
     * FromInternalImportPolicy.
     *
     */
    private static final class FromReflectorClientImportPolicy extends FromInternalImportPolicy {
        // FIXME: override if necessary
    }

    private static final Map<PeerRole, AbstractImportPolicy> POLICIES;

    static {
        final Map<PeerRole, AbstractImportPolicy> p = new EnumMap<PeerRole, AbstractImportPolicy>(PeerRole.class);
        p.put(PeerRole.Ebgp, new FromExternalImportPolicy());
        p.put(PeerRole.Ibgp, new FromInternalImportPolicy());
        p.put(PeerRole.RrClient, new FromReflectorClientImportPolicy());
        POLICIES = p;
    }

    static AbstractImportPolicy forRole(final PeerRole peerRole) {
        return POLICIES.get(peerRole);
    }

    abstract ContainerNode effectiveAttributes(ContainerNode attributes);
}