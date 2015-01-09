package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;

/**
 * @param <T> NLRI type
 */
final class RibTableResyncContext<T> {
    private final Map<T, RibTableEntry> uncheckedEntries;
    private final Collection<OffsetMap> peerOffsetMaps;

    RibTableResyncContext(final Map<T, RibTableEntry> uncheckedEntries, final Collection<OffsetMap> peerOffsetMaps) {
        this.uncheckedEntries = Preconditions.checkNotNull(uncheckedEntries);
        this.peerOffsetMaps = Preconditions.checkNotNull(peerOffsetMaps);
    }

    boolean isStaleEntry(final RibTableEntry entry) {
        return this.peerOffsetMaps.contains(entry.getRouteSources());
    }

}
