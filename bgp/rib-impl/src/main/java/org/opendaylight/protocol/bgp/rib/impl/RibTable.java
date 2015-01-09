/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.primitives.UnsignedInteger;
import com.romix.scala.collection.concurrent.TrieMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A single table in the Routing Information Base.
 *
 * @param <T> NLRI type
 */
final class RibTable<T> {
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
    private final TrieMap<T, RibTableEntry> entries = new TrieMap<>();
    private final BlockingQueue<T> updatedEntries = new LinkedBlockingQueue<>();

    private synchronized RibTableResyncContext<T> createResyncContext(final UnsignedInteger routerId) {
        return new RibTableResyncContext<T>(routerId, this.entries.snapshot());
    }

}
