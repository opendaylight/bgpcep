/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import com.romix.scala.collection.concurrent.TrieMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * A single table in the Routing Information Base.
 *
 * @param <T> NLRI type
 */
final class RibTable<T> {
    /**
     * An abstract operation on an entry.
     */
    private static abstract class EntryOperation {
        /**
         * Apply the operation and indicate whether a record of that entry needs
         * to go into the update queue.
         *
         * @param entry A {@link RibTableEntry} that needs to be updated
         * @param routerId Router ID of the peer which is performing the update
         * @param attributes Attributes associated with the operation
         * @return
         */
        abstract boolean apply(@Nonnull RibTableEntry<?> entry, @Nonnull UnsignedInteger routerId, @Nullable PathAttributes attributes);
    }

    /**
     * The add/update operation. Attributes have to be non-null.
     */
    private static final EntryOperation ADD_ROUTE = new EntryOperation() {
        @Override
        boolean apply(final RibTableEntry<?> entry, final UnsignedInteger routerId, final PathAttributes attributes) {
            Preconditions.checkArgument(attributes != null);
            return entry.addRoute(routerId, attributes);
        }
    };

    /**
     * The remove operation. Attributes are not used, but checked to be null.
     */
    private static final EntryOperation REMOVE_ROUTE = new EntryOperation() {
        @Override
        boolean apply(final RibTableEntry<?> entry, final UnsignedInteger routerId, final PathAttributes attributes) {
            Preconditions.checkArgument(attributes == null);
            return entry.removeRoute(routerId);
        }
    };

    /*
     * Lookup table for NLRI -> RibTableEntry. We are using a TrieMap due
     * to it being concurrent and supports efficient isolated read-write snapshots.
     *
     * We use the snapshot capability to support Graceful Restart in an efficient
     * manner. Instead of keeping a stale bit for each entry, we just mark the
     * peer as disconnected, starting the timer. If the timer expires, we just
     * walk the entries and remove the entries (without taking a snapshot).
     *
     * If the peer reconnects, we take a read-write snapshot, which will give us
     * the view of NLRIs which were present before the peer started advertising
     * routes after reconnect. We retain the snapshot until we see the End-of-RIB
     * marker. Each time we receive an advertisement/withdrawal, we remove it
     * from the snapshot in addition to normal processing.
     *
     * Once we receive the EOR, we need to purge the state routes. In order to
     * do this, we fire off a background task, which will walk all routes present
     * in the snapshot. These are guaranteed to be either not advertised by this
     * peer, or stale -- we discern the two cases by looking up the peer with the
     * route's routeSources.
     *
     * Note that peer processing needs to continue while the background task
     * works through the routes, but the updates need to be made by one thread
     * at a time. This means that while the task is running, we need to add
     * additional synchronization.
     */
    private final TrieMap<T, RibTableEntry<T>> entries = new TrieMap<>();
    private final BlockingQueue<T> updatedEntries = new LinkedBlockingQueue<>();

    private synchronized RibTableResyncContext<T> createResyncContext(final UnsignedInteger routerId) {
        return new RibTableResyncContext<T>(routerId, this.entries.snapshot());
    }

    private final void updateRoutes(final EntryOperation op, final UnsignedInteger routerId, final PathAttributes attributes, final T... routes) {
        Preconditions.checkNotNull(routerId);

        final Collection<T> toNotify = new ArrayList<>(routes.length);

        for (final T r : routes) {
            final RibTableEntry<T> entry = entries.get(r);

            // FIXME: deal with the entry not being present

            if (op.apply(entry, routerId, attributes)) {
                toNotify.add(r);
            }
        }

        updatedEntries.addAll(toNotify);
    }

    /**
     * Advertise a particular set of routes sharing attributes.
     *
     * @param routerId Router ID of the peer whence the update can from, must not be null
     * @param attributes Attribute object, must not be null
     * @param routes One or more routes
     */
    private void advertize(@Nonnull final UnsignedInteger routerId, @Nonnull final PathAttributes attributes, final T... routes) {
        updateRoutes(ADD_ROUTE, routerId, attributes, routes);
    }

    /**
     * Withdraw a set of routes.
     *
     * @param routerId Router ID of the peer whence the update can from, must not be null
     * @param routes One or more routes
     */
    private void withdraw(@Nonnull final UnsignedInteger routerId, final T... routes) {
        updateRoutes(REMOVE_ROUTE, routerId, null, routes);
    }
}
