/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import javax.annotation.concurrent.GuardedBy;
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

    /*
     * Dynamically sized at 2x routeSinks.size(). Produced attributes
     * (by selection) start at 0 forward. Consumed (by peer) start
     * at the end going backwards.
     */
    private final PathAttributes[] sinkAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private final OffsetMap routeSinks = OffsetMap.EMPTY;

    /*
     * Dynamically sized at 2x routeSources.size(). Produced attributes
     * (by peer) start at 0 forward. Consumed (by selector) start
     * at the end going backwards.
     */
    private PathAttributes[] sourceAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private final OffsetMap routeSources = OffsetMap.EMPTY;
    private PathAttributes currentAttribute;

    boolean containsSource(final Ipv4Address routerId) {
        return this.routeSources.offsetOf(routerId) >= 0;
    }

    synchronized void addRoute(final Ipv4Address routerId, final PathAttributes attributes) {
        // TODO: lock-free route overwrite when we do not have to reformat the attributes?

        int offset = this.routeSources.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newSources = this.routeSources.with(routerId);
            final PathAttributes[] newAttributes = newSources.allocateArray();
            offset = newSources.offsetOf(routerId);

            // FIXME: reformat attributes

            this.sourceAttributes = newAttributes;
        }

        this.routeSources.setProduced(this.sourceAttributes, offset, attributes);
    }

    synchronized void removeRoute(final Ipv4Address routerId) {
        final int offset = this.routeSources.offsetOf(routerId);
        if (offset >= 0) {
            this.routeSources.setProduced(this.sourceAttributes, offset, null);
        }
    }
}
