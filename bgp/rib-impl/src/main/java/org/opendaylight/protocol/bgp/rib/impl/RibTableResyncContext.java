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
import java.util.concurrent.ConcurrentMap;

/**
 * A resynchronization context pertaining to a single RibTable. A peer undergoing
 * a graceful restart (and session reestablishment) will have an instance of this
 * class for each advertised table.
 *
 * @param <T> NLRI type
 */
final class RibTableResyncContext<T> {
    private final ConcurrentMap<T, RibTableEntry> uncheckedEntries;
    private final UnsignedInteger routerId;

    RibTableResyncContext(final UnsignedInteger routerId, final ConcurrentMap<T, RibTableEntry> uncheckedEntries) {
        this.uncheckedEntries = Preconditions.checkNotNull(uncheckedEntries);
        this.routerId = Preconditions.checkNotNull(routerId);
    }

    /**
     * Check if a route (and corresponding entry) are stale and should be pruned.
     * This assumes that a concurrent update cannot happen (but may have happened
     * since the caller has started iterating).
     *
     * @param nlri Route NLRI
     * @param entry Route entry
     * @return True if the route should be pruned.
     */
    boolean isStaleEntry(final T nlri, final RibTableEntry entry) {
        /*
         * First check if the routerId is in the entry. This prevents unnecessary
         * churn in the snapshot caused by removal of routes not advertised by the
         * peer.
         *
         * TODO: this is a O(log2(n)) lookup, based on String hash/equals.
         *       We could optimize it by maintaining a collection of OffsetMaps
         *       known to contain the routerId and checking against that first.
         */
        if (!entry.containsSource(this.routerId)) {
            return false;
        }

        /*
         * Since this is the RibCleanupTask iterating on a stable snapshot,
         * the entry may have been removed by a normal peer advertisement which
         * occurred while the task was not holding the exclusion lock. Take
         * advantage of the concurrent nature of uncheckedEntries -- a remove()
         * will return true precisely once.
         */
        return this.uncheckedEntries.remove(nlri, entry);
    }

    /**
     * Note that a particular route has been updated by the peer. If the route
     * has been updated or pruned as stale, this method does nothing.
     *
     * @param nlri route updated
     */
    void entryUpdated(final T nlri) {
        this.uncheckedEntries.remove(nlri);
    }
}
