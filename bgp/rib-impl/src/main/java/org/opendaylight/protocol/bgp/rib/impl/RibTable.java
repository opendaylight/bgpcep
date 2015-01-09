package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.romix.scala.collection.concurrent.TrieMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * @param <T> NLRI type
 */
public class RibTable<T> {
    @GuardedBy("this")
    private final Map<Set<Ipv4Address>, OffsetMap> offsetMaps = new WeakHashMap<>();

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
     * peer, or stale.
     *
     * To efficiently discern the two cases, we construct a collection of all
     * OffsetMaps which contain the peer. As we walk the routes, we check whether
     * the route's routeSources is within the collection.
     *
     * Note that peer processing needs to continue while the background task
     * works through the routes, but the updates need to be made by one thread
     * at a time. This means that while the task is running, we need to add
     * additional synchronization.
     */
    private final TrieMap<T, RibTableEntry> entries = new TrieMap<>();
    private final BlockingQueue<T> updatedEntries = new LinkedBlockingQueue<>();

    private synchronized RibTableResyncContext<T> createResyncContext(final Ipv4Address routerId) {
        final Map<T, RibTableEntry> uncheckedEntries = this.entries.snapshot();
        final Builder<OffsetMap> builder = ImmutableSet.builder();

        for (final Entry<Set<Ipv4Address>, OffsetMap> e : this.offsetMaps.entrySet()) {
            if (e.getKey().contains(routerId)) {
                builder.add(e.getValue());
            }
        }

        return new RibTableResyncContext<T>(uncheckedEntries, builder.build());
    }

}
