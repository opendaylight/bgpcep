/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * A single route entry inside a {@link RibTable}. Maintains the attributes of
 * from all contributing peers as well as state of all consuming peers. The
 * information is stored in arrays with a shared map of offsets for peers to
 * allow lookups. This is needed to maintain low memory overhead in face of
 * large number of routes and peers, where individual object overhead becomes
 * the dominating factor.
 *
 * FIXME: attributes are never shrunk. Design and implement lazy shrinking
 *        logic, as we cannot shrink eagerly without incurring large overheads
 *        on route flaps.
 */
final class RibTableEntry {
    private static final PathAttributes[] EMPTY_ATTRIBUTES = new PathAttributes[0];

    // Entries produced by the selection process, consumed by the peer.
    private final PathAttributes[] sinkAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private final OffsetMap routeSinks = OffsetMap.EMPTY;

    // Entries produced by the peer, consumed by the selection process.
    @GuardedBy("this")
    private PathAttributes[] sourceAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private OffsetMap routeSources = OffsetMap.EMPTY;
    @GuardedBy("this")
    private BestPath current;

    boolean containsSource(final Ipv4Address routerId) {
        return this.routeSources.offsetOf(routerId) >= 0;
    }

    private synchronized void addRoute(final Ipv4Address routerId, final PathAttributes attributes) {
        // TODO: lock-free route overwrite when we do not have to reformat the attributes?

        int offset = this.routeSources.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newSources = this.routeSources.with(routerId);
            offset = newSources.offsetOf(routerId);

            final PathAttributes[] newAttributes = newSources.reformat(this.routeSources, this.sourceAttributes, offset);
            this.sourceAttributes = newAttributes;
            this.routeSources = newSources;
        }

        this.routeSources.setProduced(this.sourceAttributes, offset, attributes);
    }

    private synchronized void removeRoute(final Ipv4Address routerId) {
        final int offset = this.routeSources.offsetOf(routerId);
        if (offset >= 0) {
            this.routeSources.setProduced(this.sourceAttributes, offset, null);
        }
    }

    private synchronized BestPath findCandidate(final AsNumber localAs) {
        final BestPathSelector selector = new BestPathSelector(localAs);

        for (int i = 0; i < this.routeSources.size(); ++i) {
            final Ipv4Address routerId = this.routeSources.getRouterId(i);
            final PathAttributes attributes = this.routeSources.getProduced(this.sourceAttributes, i);

            /*
             * TODO: optimize flaps by making sure we consider stability of currently-selected route
             */
            selector.processPath(routerId, attributes);
        }

        final BestPath newState = selector.result();
        current = newState;
        return newState;
    }
}
