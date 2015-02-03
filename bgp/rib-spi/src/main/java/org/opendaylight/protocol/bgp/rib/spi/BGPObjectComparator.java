/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs.RIBEntryData;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;

/**
 * This comparator is intended to implement BGP Best Path Selection algorithm, as described at
 *
 * @see http://www.cisco.com/en/US/tech/tk365/technologies_tech_note09186a0080094431.shtml
 *
 * @param <T> Actual object state reference
 */
public final class BGPObjectComparator implements Comparator<RIBEntryData<?, ?, ?>>, Serializable {

    private static final long serialVersionUID = 3299599519482155374L;

    private final AsNumber ourAS;

    public BGPObjectComparator(final AsNumber ourAs) {
        this.ourAS = Preconditions.checkNotNull(ourAs);
    }

    @Override
    public int compare(final RIBEntryData<?, ?, ?> newObject, final RIBEntryData<?, ?, ?> oldObject) {
        if (newObject == oldObject) {
            return 0;
        }
        if (newObject == null) {
            return 1;
        }
        if (oldObject == null) {
            return -1;
        }

        final PathAttributes newPath = newObject.getPathAttributes();
        final PathAttributes oldPath = oldObject.getPathAttributes();
        if (newPath.equals(oldPath) && Arrays.equals(newObject.getPeer().getRawIdentifier(), oldObject.getPeer().getRawIdentifier())) {
            return 0;
        }

        // 1. prefer path with accessible nexthop
        // - we assume that all nexthops are accessible

        // 2. prefer path with higher LOCAL_PREF
        if ((newPath.getLocalPref() != null || oldPath.getLocalPref() != null)
                && (newPath.getLocalPref() != null && !newPath.getLocalPref().equals(oldPath.getLocalPref()))) {
            return newPath.getLocalPref().getPref().compareTo(oldPath.getLocalPref().getPref());
        }

        // 3. prefer learned path
        // - we assume that all paths are learned

        // 4. prefer the path with the shortest AS_PATH.
        if (!newPath.getAsPath().equals(oldPath.getAsPath())) {
            final Integer i1 = countAsPath(newPath.getAsPath().getSegments());
            final Integer i2 = countAsPath(oldPath.getAsPath().getSegments());
            return i2.compareTo(i1);
        }

        // 5. prefer the path with the lowest origin type
        // - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
        if (!newPath.getOrigin().equals(oldPath.getOrigin())) {
            if (newPath.getOrigin().getValue().equals(BgpOrigin.Igp)) {
                return 1;
            }
            if (oldPath.getOrigin().getValue().equals(BgpOrigin.Igp)) {
                return -1;
            }
            if (newPath.getOrigin().getValue().equals(BgpOrigin.Egp)) {
                return 1;
            } else {
                return -1;
            }
        }

        // 6. prefer the path with the lowest multi-exit discriminator (MED)
        if ((newPath.getMultiExitDisc() != null || oldPath.getMultiExitDisc() != null)
                && (newPath.getMultiExitDisc() != null && !newPath.getMultiExitDisc().equals(oldPath.getMultiExitDisc()))) {
            return oldPath.getMultiExitDisc().getMed().compareTo(newPath.getMultiExitDisc().getMed());
        }

        // 7. prefer eBGP over iBGP paths
        // EBGP is peering between two different AS, whereas IBGP is between same AS (Autonomous System).
        final AsNumber first = getPeerAs(newPath.getAsPath().getSegments());
        final AsNumber second = getPeerAs(oldPath.getAsPath().getSegments());
        if ((first != null || second != null) && (first != null && !first.equals(second))) {
            if (first.equals(this.ourAS)) {
                return -1;
            }
            if (second == null || second.equals(this.ourAS)) {
                return 1;
            }
        }

        // 8. Prefer the path with the lowest IGP metric to the BGP next hop.
        // - no next hop metric is advertized

        // 9. When both paths are external, prefer the path that was received first (the oldest one).
        // if (first.equals(this.ourAS) && second.equals(this.ourAS)) {
        // FIXME: do we have a way how to determine which one was received first?

        // 10. Prefer the route that comes from the BGP router with the lowest router ID.
        // The router ID is the highest IP address on the router, with preference given to loopback addresses.
        // If a path contains route reflector (RR) attributes, the originator ID is substituted for the router ID in the
        // path selection process.

        // RFC5004 states that this algorithm should end here and select existing path over new path in the
        // best path selection process. Benefits are listed in the RFC: @see http://tools.ietf.org/html/rfc500
        // - This algorithm SHOULD NOT be applied when either path is from a BGP Confederation peer.
        //  - not applicable, we don't deal with confederation peers
        // - The algorithm SHOULD NOT be applied when both paths are from peers with an identical BGP identifier (i.e., there exist parallel BGP sessions between two BGP speakers).
        //  - not applicable, BUG-2631 prevents parallel sessions to be created.

        return -1;
    }

    private static int countAsPath(final List<Segments> segments) {
        // an AS_SET counts as 1, no matter how many ASs are in the set.
        int count = 0;
        boolean setPresent = false;
        for (final Segments s : segments) {
            if (s.getCSegment() instanceof ASetCase) {
                setPresent = true;
            } else {
                final AListCase list = (AListCase) s.getCSegment();
                count += list.getAList().getAsSequence().size();
            }
        }
        return (setPresent) ? ++count : count;
    }

    private static AsNumber getPeerAs(final List<Segments> segments) {
        if (segments.size() == 0) {
            return null;
        }
        final AListCase first = (AListCase) segments.get(0).getCSegment();
        return first.getAList().getAsSequence().get(0).getAs();
    }

    @VisibleForTesting
    public static int compareByteArrays(final byte[] byteOne, final byte[] byteTwo) {
        for (int i = 0; i < byteOne.length; i++) {
            final int res = Byte.compare(byteOne[i], byteTwo[i]);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }
}
