package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

final class RibTableEntry {
    private static final PathAttributes[] EMPTY_ATTRIBUTES = new PathAttributes[0];

    @GuardedBy("this")
    private final OffsetMap routeSources = OffsetMap.EMPTY;
    @GuardedBy("this")
    private final OffsetMap routeSinks = OffsetMap.EMPTY;

    /*
     * Dynamically sized at 2x routeSinks.size(). Produced attributes
     * (by selection) start at 0 forward. Consumed (by peer) start
     * at the end going backwards.
     */
    private final PathAttributes[] sinkAttributes = EMPTY_ATTRIBUTES;

    /*
     * Dynamically sized at 2x routeSources.size(). Produced attributes
     * (by peer) start at 0 forward. Consumed (by selector) start
     * at the end going backwards.
     */
    private final PathAttributes[] sourceAttributes = EMPTY_ATTRIBUTES;
    private PathAttributes currentAttribute;

}
