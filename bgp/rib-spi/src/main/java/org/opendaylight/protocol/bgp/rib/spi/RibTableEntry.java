package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

final class RibTableEntry {
    @GuardedBy("this")
    private OffsetMap routeSources;
    private PathAttributes[] sourceAttributes; // 2x size of routeSources
    private PathAttributes currentAttribute;

    @GuardedBy("this")
    private OffsetMap routeSinks;
    private PathAttributes[] sinkAttributes; // 2x size of routeSinze

}
