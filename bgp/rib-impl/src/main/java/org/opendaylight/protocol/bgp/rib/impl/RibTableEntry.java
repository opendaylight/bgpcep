/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import java.util.Collection;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
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
 *
 * @param <T> NLRI type
 */
final class RibTableEntry<T> {
    private static final PathAttributes[] EMPTY_ATTRIBUTES = new PathAttributes[0];

    // Entries produced by the selection process, consumed by the peer.
    @GuardedBy("this")
    private PathAttributes[] sinkAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private OffsetMap routeSinks = OffsetMap.EMPTY;

    // Entries produced by the peer, consumed by the selection process.
    @GuardedBy("this")
    private PathAttributes[] sourceAttributes = EMPTY_ATTRIBUTES;
    @GuardedBy("this")
    private OffsetMap routeSources = OffsetMap.EMPTY;
    @GuardedBy("this")
    private BestPath current;

    boolean containsSource(final UnsignedInteger routerId) {
        return this.routeSources.offsetOf(routerId) >= 0;
    }

    @GuardedBy("this")
    private boolean updateRoute(final int offset, final PathAttributes attributes) {
        // FIXME: check this for correctness
        this.routeSources.setProduced(this.sourceAttributes, offset, attributes);
        return this.routeSources.getConsumed(this.sourceAttributes, offset) == attributes;
    }

    synchronized boolean addRoute(final UnsignedInteger routerId, final PathAttributes attributes) {
        // TODO: lock-free route overwrite when we do not have to reformat the attributes?

        int offset = this.routeSources.offsetOf(routerId);
        if (offset < 0) {
            final OffsetMap newSources = this.routeSources.with(routerId);
            offset = newSources.offsetOf(routerId);

            final PathAttributes[] newAttributes = newSources.reformat(this.routeSources, this.sourceAttributes, offset);
            this.sourceAttributes = newAttributes;
            this.routeSources = newSources;
        }

        return updateRoute(offset, attributes);
    }

    synchronized boolean removeRoute(final UnsignedInteger routerId) {
        final int offset = this.routeSources.offsetOf(routerId);
        if (offset < 0) {
            return false;
        }

        return updateRoute(offset, null);
    }

    /**
     * Perform route selection. The explicit assumption is that this method is never
     * called from a single thread.
     */
    private void selectRoute(final T nlri, final AsNumber localAs, final Collection<RouteSink> potentialSinks) {
        final BestPathSelector selector = new BestPathSelector(localAs);

        /*
         * Select the best route. This part must run synchronized, so we do not get interference
         * from route sources.
         */
        for (int i = 0; i < this.routeSources.size(); ++i) {
            final UnsignedInteger routerId = this.routeSources.getRouterId(i);
            final PathAttributes attributes = this.routeSources.getProduced(this.sourceAttributes, i);

            /*
             * TODO: optimize flaps by making sure we consider stability of currently-selected route
             */
            selector.processPath(routerId, attributes);

            this.routeSources.setConsumed(this.sourceAttributes, i, attributes);
        }

        /*
         * We have new state. Check if the attributes have changed and suppress notifications
         * if they have not.
         *
         * FIXME: we can be a bit more thorough here, but that needs a bit of analysis.
         */
        final BestPath newState = selector.result();
        final PathAttributes attrs = newState.getAttributes();
        if (current != null && current.getAttributes() == attrs) {
            return;
        }

        // Real update, we need to run the export policy and update peers
        current = newState;
        for (RouteSink s : potentialSinks) {

            // FIXME: RFC 4456 needs us to correlate whether a route comes from a client or a non-client,
            //        and distribute accordingly. https://tools.ietf.org/html/rfc4456#section-6
            final PathAttributes newProd;
            if (s.canConsumeRoute(attrs)) {
                newProd = attrs;
            } else {
                newProd = null;
            }

            int offset = routeSinks.offsetOf(s.getRouterId());
            final PathAttributes oldProd;
            if (offset >= 0) {
                oldProd = routeSinks.getProduced(sinkAttributes, offset);
            } else {
                oldProd = null;
            }

            // Non-advertised now and non-advertised after -- nothing to do.
            if (newProd == null && oldProd == null) {
                continue;
            }

            /*
             * At this point we know we need to perform some change in advertisement.
             * If the route was already-advertised, we need to re-advertise.  Withdrawal
             * equals to a null advertisement.
             *
             * If the route was not advertised to this peer, we may need to re-format, so
             * take care of that first.
             */
            if (offset < 0) {
                final OffsetMap newSinks = this.routeSinks.with(s.getRouterId());
                offset = newSinks.offsetOf(s.getRouterId());

                final PathAttributes[] newAttributes = newSinks.reformat(this.routeSinks, this.sinkAttributes, offset);
                this.sinkAttributes = newAttributes;
                this.routeSinks = newSinks;
            }

            // Publish the production first...
            routeSinks.setProduced(sinkAttributes, offset, newProd);

            /*
             * Now the tricky part. It it possible the consumer has not yet reacted
             * to the previous change in this route entry (for example because it cannot
             * keep up). If that is the case, it will pick up the production anyway
             * without us notifying it again.
             *
             * Direct object comparison here is for performance reasons.
             */
            final PathAttributes currentConsumed = routeSinks.getConsumed(sinkAttributes, offset);
            if (currentConsumed == oldProd) {
                s.routeUpdated(nlri);
            }
        }
    }
}
