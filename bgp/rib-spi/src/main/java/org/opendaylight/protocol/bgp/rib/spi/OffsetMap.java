package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

final class OffsetMap {
    private static final Comparator<Ipv4Address> IPV4_COMPARATOR = new Comparator<Ipv4Address>() {
        @Override
        public int compare(final Ipv4Address o1, final Ipv4Address o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };
    private final Ipv4Address[] routerIds;

    OffsetMap(final Set<Ipv4Address> routerIds) {
        final Ipv4Address[] array = routerIds.toArray(new Ipv4Address[0]);
        Arrays.sort(array, IPV4_COMPARATOR);
        this.routerIds = array;
    }

    int offsetOf(final Ipv4Address routerId) {
        final int ret = Arrays.binarySearch(this.routerIds, routerId, IPV4_COMPARATOR);
        Preconditions.checkArgument(ret >= 0, "Unknown router ID %s", routerId);
        return ret;
    }

    Ipv4Address routerIdAt(final int offset) {
        return this.routerIds[offset];
    }
}
