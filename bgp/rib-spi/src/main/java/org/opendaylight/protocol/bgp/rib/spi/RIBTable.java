package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * @param <T> NLRI type
 */
public class RIBTable<T> {
    private final Map<Set<Ipv4Address>, OffsetMap> offsetMaps = new WeakHashMap<>();
    private final Map<T, RibTableEntry> entries = new ConcurrentHashMap<>();
    private final BlockingQueue<T> updatedEntries = new LinkedBlockingQueue<>();


}
