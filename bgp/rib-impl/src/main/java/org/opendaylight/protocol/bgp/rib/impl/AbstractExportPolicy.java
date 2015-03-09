/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Defines the internal hooks invoked when a new route appears.
 */
abstract class AbstractExportPolicy {
    // Invoked on routes which we send outside of our home AS
    private static final class ToExternalExportPolicy extends AbstractExportPolicy {
        @Override
        ContainerNode effectiveAttributes(final PeerRole sourceRole, final ContainerNode attributes) {
            // FIXME: filter all non-transitive attributes
            // FIXME: but that may end up hurting our informedness
            // FIXME: prepend local AS to add our AS into AS_PATH

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
                break;
            }
            return attributes;
        }
    }

    // Invoked on routes which we send to our normal home AS peers.
    private static class ToInternalExportPolicy extends AbstractExportPolicy {
        protected static ContainerNode prependClusterId(final ContainerNode attributes) {
            // FIXME: prepend local CLUSTER_ID to CLUSTER_LIST
            return attributes;
        }

        // Attributes when reflecting a route
        protected static ContainerNode toClientAttributes(final ContainerNode attributes) {
            // TODO: (defensiveness) verify ORIGINATOR_ID (should have been set)

            return prependClusterId(attributes);
        }

        @Override
        ContainerNode effectiveAttributes(final PeerRole sourceRole, final ContainerNode attributes) {
            // Implement filtering according to <a ref="http://tools.ietf.org/html/rfc4456#section-8"/>.

            switch (sourceRole) {
            case Ebgp:
                // eBGP -> Non-Client iBGP, propagate
                return attributes;

            case Ibgp:
                // Non-Client iBGP -> Non-Client iBGP, block
                return null;

            case RrClient:
                // Client iBGP -> Non-Client iBGP, reflect
                return toClientAttributes(attributes);
            default:
                break;
            }
            return attributes;
        }
    }

    /**
     * Invoked on routes which we send to our reflector peers. This is a special-case of
     * FromInternalImportPolicy.
     */
    private static final class ToReflectorClientExportPolicy extends AbstractExportPolicy {
        @Override
        ContainerNode effectiveAttributes(final PeerRole sourceRole, final ContainerNode attributes) {
            switch (sourceRole) {
            case Ebgp:
                // eBGP -> Client iBGP, propagate
                return attributes;
            case Ibgp:
                // Non-Client iBGP -> Client iBGP, reflect
                // FIXME: add ORIGINATOR_ID

                return ToInternalExportPolicy.prependClusterId(attributes);
            case RrClient:
                // Client iBGP -> Client iBGP, reflect
                return ToInternalExportPolicy.toClientAttributes(attributes);
            default:
                throw new IllegalStateException("Unhandled source role " + sourceRole);
            }
        }
    }

    static final Map<PeerRole, AbstractExportPolicy> POLICIES;

    static {
        final Map<PeerRole, AbstractExportPolicy> p = new EnumMap<PeerRole, AbstractExportPolicy>(PeerRole.class);
        p.put(PeerRole.Ebgp, new ToExternalExportPolicy());
        p.put(PeerRole.Ibgp, new ToInternalExportPolicy());
        p.put(PeerRole.RrClient, new ToReflectorClientExportPolicy());
        POLICIES = Maps.immutableEnumMap(p);
    }

    static AbstractExportPolicy forRole(final PeerRole peerRole) {
        return POLICIES.get(peerRole);
    }

    /**
     * Transform outgoing attributes according to policy.
     *
     * @param sourceRole role of the peer which originated the routes
     * @param attributes outgoing attributes
     * @return Filtered attributes, or null if the advertisement should be ignored.
     */
    abstract ContainerNode effectiveAttributes(PeerRole sourceRole, ContainerNode attributes);
}