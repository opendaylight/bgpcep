/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;

final class BestPathSelector {
    private final AsNumber ourAs;
    private Ipv4Address bestOriginatorId = null;
    private PathAttributes bestAttrs = null;

    BestPathSelector(final AsNumber ourAs) {
        this.ourAs = Preconditions.checkNotNull(ourAs);
    }

    void processPath(final Ipv4Address routerId, final PathAttributes attrs) {
        Preconditions.checkNotNull(routerId, "Router ID may not be null");

        // Consider only non-null attributes
        if (attrs != null) {
            /*
             * RFC 4456 mandates the use of Originator IDs instead of Router ID for
             * selection purposes.
             */
            final Ipv4Address originatorId;
            if (attrs.getOriginatorId() != null) {
                originatorId = attrs.getOriginatorId().getOriginator();
            } else {
                originatorId = routerId;
            }

            /*
             * Store the new details if we have nothing stored or when the selection algorithm indicates new details
             * are better.
             */
            if (bestOriginatorId == null || selectPath(routerId, attrs)) {
                bestOriginatorId = originatorId;
                bestAttrs = attrs;
            }
        }
    }

    BestPath result() {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean selectPath(final @Nonnull Ipv4Address originatorId, final @Nonnull PathAttributes attrs) {
        // 1. prefer path with accessible nexthop
        // - we assume that all nexthops are accessible

        /*
         * 2. prefer path with higher LOCAL_PREF
         *
         * FIXME: for eBGP cases (when the LOCAL_PREF is missing), we should assign a policy-based preference
         *        before we ever get here.
         */
        if (bestAttrs.getLocalPref() == null && attrs.getLocalPref() != null) {
            return true;
        }
        if (bestAttrs.getLocalPref() != null && attrs.getLocalPref() == null) {
            return false;
        }
        if (attrs.getLocalPref() != null) {
            if (attrs.getLocalPref().getPref() > bestAttrs.getLocalPref().getPref()) {
                return true;
            }
        }

        // 3. prefer learned path
        // - we assume that all paths are learned

        // 4. prefer the path with the shortest AS_PATH.
        if (!bestAttrs.getAsPath().equals(attrs.getAsPath())) {
            // FIXME: this is something we can cache...
            final int bap = countAsPath(bestAttrs.getAsPath().getSegments());
            final int nap = countAsPath(attrs.getAsPath().getSegments());
            if (bap > nap) {
                return true;
            }
        }

        // 5. prefer the path with the lowest origin type
        // - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
        if (!bestAttrs.getOrigin().equals(attrs.getOrigin())) {
            final BgpOrigin bo = bestAttrs.getOrigin().getValue();
            final BgpOrigin no = attrs.getOrigin().getValue();

            // This trick relies on the order in which the values are declared in the model.
            if (no.ordinal() < bo.ordinal()) {
                return true;
            }
        }

        // FIXME: we should be able to cache the best AS
        final AsNumber bestAs = getPeerAs(bestAttrs.getAsPath().getSegments());
        final AsNumber newAs = getPeerAs(attrs.getAsPath().getSegments());

        /*
         * Checks 6 and 7 are mutually-exclusive, as MEDs are comparable
         * only when the routes originated from the same AS. On the other
         * hand, when they are from the same AS, they are in the same iBGP/eBGP
         * relationship.
         *
         */
        if (bestAs.equals(newAs)) {
            // 6. prefer the path with the lowest multi-exit discriminator (MED)
            if (bestAttrs.getMultiExitDisc() != null || attrs.getMultiExitDisc() != null) {
                final Long bmed = bestAttrs.getMultiExitDisc().getMed();
                final Long nmed = attrs.getMultiExitDisc().getMed();
                if (nmed < bmed) {
                    return true;
                }
            }
        } else {
            /*
             * 7. prefer eBGP over iBGP paths
             *
             * EBGP is peering between two different AS, whereas IBGP is between same AS (Autonomous System),
             * so we just compare the AS numbers to our AS.
             *
             * FIXME: we should know this information from the peer directly.
             */
            if (!ourAs.equals(bestAs) && ourAs.equals(newAs)) {
                return true;
            }
        }

        // 8. Prefer the path with the lowest IGP metric to the BGP next hop.
        // - no next hop metric is advertised

        /*
         * 9. When both paths are external, prefer the path that was received first (the oldest one).
         *
         * FIXME: we do not want to store an explicit timer for each set due to performance/memory
         *        constraints. Our caller has the information about which attributes have changed
         *        since the selection process has ran last time, which may be a good enough approximation,
         *        but its properties need to be analyzed.
         */

        /*
         * 10. Prefer the route that comes from the BGP router with the lowest router ID.
         *
         * This is normally guaranteed by the iteration order of our caller, which runs selection
         * in the order of increasing router ID, but Route Reflection throws a wrench into that.
         */

        // FIXME: this is wrong, we need to use an integer for this
        if (bestOriginatorId.getValue().compareTo(originatorId.getValue()) > 0) {
            return true;
        }

        // 11. prefer the path with the minimum cluster list length
        // FIXME: we should be able to cache cluster length
        final int bestCluster = getClusterLength(bestAttrs);
        final int newCluster = getClusterLength(attrs);
        if (newCluster < bestCluster) {
            return true;
        }

        // 12. Prefer the path that comes from the lowest neighbor address.
        // FIXME: do we know this?
        return false;
    }

    private static int getClusterLength(final PathAttributes attrs) {
        final ClusterId cid = attrs.getClusterId();
        return cid == null ? 0 : cid.getCluster().size();
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
        return (setPresent) ? count + 1 : count;
    }

    private static AsNumber getPeerAs(final List<Segments> segments) {
        if (segments.isEmpty()) {
            return null;
        }

        final AListCase first = (AListCase) segments.get(0).getCSegment();
        return first.getAList().getAsSequence().get(0).getAs();
    }

}
