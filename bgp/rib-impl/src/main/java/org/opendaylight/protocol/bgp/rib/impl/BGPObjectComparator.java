/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
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
final class BGPObjectComparator implements Comparator<PathAttributes> {
    private final byte[] localId, remoteId;
    private final AsNumber ourAS;

    public BGPObjectComparator(final AsNumber ourAs, final Ipv4Address localId, final Ipv4Address remoteId) {
        this.ourAS = Preconditions.checkNotNull(ourAs);
        this.localId = InetAddresses.forString(localId.getValue()).getAddress();
        this.remoteId = InetAddresses.forString(remoteId.getValue()).getAddress();
    }

    @Override
    public int compare(final PathAttributes o1, final PathAttributes o2) {
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        if (o1.equals(o2) && Arrays.equals(this.localId, this.remoteId)) {
            return 0;
        }
        // 1. prefer path with accessible nexthop
        // - we assume that all nexthops are accessible

        // 2. prefer path with higher LOCAL_PREF
        if ((o1.getLocalPref() != null || o2.getLocalPref() != null)
                && (o1.getLocalPref() != null && !o1.getLocalPref().equals(o2.getLocalPref()))) {
            return o1.getLocalPref().getPref().compareTo(o2.getLocalPref().getPref());
        }

        // 3. prefer learned path
        // - we assume that all paths are learned

        // 4. prefer the path with the shortest AS_PATH.
        if (!o1.getAsPath().equals(o2.getAsPath())) {
            final Integer i1 = countAsPath(o1.getAsPath().getSegments());
            final Integer i2 = countAsPath(o2.getAsPath().getSegments());
            return i2.compareTo(i1);
        }

        // 5. prefer the path with the lowest origin type
        // - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
        if (!o1.getOrigin().equals(o2.getOrigin())) {
            if (o1.getOrigin().getValue().equals(BgpOrigin.Igp)) {
                return 1;
            }
            if (o2.getOrigin().getValue().equals(BgpOrigin.Igp)) {
                return -1;
            }
            if (o1.getOrigin().getValue().equals(BgpOrigin.Egp)) {
                return 1;
            } else {
                return -1;
            }
        }

        // 6. prefer the path with the lowest multi-exit discriminator (MED)
        if ((o1.getMultiExitDisc() != null || o2.getMultiExitDisc() != null)
                && (o1.getMultiExitDisc() != null && !o1.getMultiExitDisc().equals(o2.getMultiExitDisc()))) {
            return o2.getMultiExitDisc().getMed().compareTo(o1.getMultiExitDisc().getMed());
        }

        // 7. prefer eBGP over iBGP paths
        // EBGP is peering between two different AS, whereas IBGP is between same AS (Autonomous System).
        final AsNumber first = getPeerAs(o1.getAsPath().getSegments());
        final AsNumber second = getPeerAs(o2.getAsPath().getSegments());
        if ((first != null || second != null) && (first != null && !first.equals(second))) {
            if (first == null || first.equals(this.ourAS)) {
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
        // }

        // 10. Prefer the route that comes from the BGP router with the lowest router ID.
        // The router ID is the highest IP address on the router, with preference given to loopback addresses.
        // If a path contains route reflector (RR) attributes, the originator ID is substituted for the router ID in the
        // path selection process.
        byte[] oid1 = this.localId;
        byte[] oid2 = this.remoteId;
        if (o1.getOriginatorId() != null) {
            oid1 = InetAddresses.forString(o1.getOriginatorId().getValue()).getAddress();
        }
        if (o2.getOriginatorId() != null) {
            oid2 = InetAddresses.forString(o2.getOriginatorId().getValue()).getAddress();
        }
        if (!Arrays.equals(oid1, oid2)) {
            return compareByteArrays(oid1, oid2);
        }
        // 11. prefer the path with the minimum cluster list length
        int cluster1 = 0;
        int cluster2 = 0;
        if (o1.getClusterId() != null) {
            cluster1 = o1.getClusterId().size();
        }
        if (o2.getClusterId() != null) {
            cluster2 = o2.getClusterId().size();
        }
        if (cluster1 != cluster2) {
            return ((Integer) cluster1).compareTo(cluster2);
        }

        // 12. Prefer the path that comes from the lowest neighbor address.
        // FIXME: do we know this?

        return 0;
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
