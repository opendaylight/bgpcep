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
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;

final class BestPathSelector {
    private final AsNumber ourAs;
    private UnsignedInteger bestOriginatorId = null;
    private UnsignedInteger bestRouterId = null;
    private PathAttributes bestAttrs = null;

    BestPathSelector(final AsNumber ourAs) {
        this.ourAs = Preconditions.checkNotNull(ourAs);
    }

    void processPath(final UnsignedInteger routerId, final PathAttributes attrs) {
        Preconditions.checkNotNull(routerId, "Router ID may not be null");

        // Consider only non-null attributes
        if (attrs != null) {
            /*
             * RFC 4456 mandates the use of Originator IDs instead of Router ID for
             * selection purposes.
             */
            final UnsignedInteger originatorId;
            if (attrs.getOriginatorId() != null) {
                originatorId = RouterIds.routerIdForAddress(attrs.getOriginatorId().getOriginator());
            } else {
                originatorId = routerId;
            }

            /*
             * Store the new details if we have nothing stored or when the selection algorithm indicates new details
             * are better.
             */
            if (this.bestOriginatorId == null || selectPath(originatorId, attrs)) {
                this.bestOriginatorId = originatorId;
                this.bestRouterId = routerId;
                this.bestAttrs = attrs;
            }
        }
    }

    BestPath result() {
        return this.bestRouterId == null ? null : new BestPath(this.bestRouterId, this.bestAttrs);
    }

    /**
     * Chooses best route according to BGP best path selection.
     *
     * @param originatorId of the new route
     * @param attrs attributes of the new route
     * @return true if the existing path is better, false if the new path is better
     */
    private boolean selectPath(final @Nonnull UnsignedInteger originatorId, final @Nonnull PathAttributes attrs) {
        // 1. prefer path with accessible nexthop
        // - we assume that all nexthops are accessible

        /*
         * 2. prefer path with higher LOCAL_PREF
         *
         * FIXME: for eBGP cases (when the LOCAL_PREF is missing), we should assign a policy-based preference
         *        before we ever get here.
         */
        if (this.bestAttrs.getLocalPref() == null && attrs.getLocalPref() != null) {
            return true;
        }
        if (this.bestAttrs.getLocalPref() != null && attrs.getLocalPref() == null) {
            return false;
        }
        if (attrs.getLocalPref() != null) {
            if (attrs.getLocalPref().getPref() > this.bestAttrs.getLocalPref().getPref()) {
                return true;
            }
        }

        // 3. prefer learned path
        // - we assume that all paths are learned

        // 4. prefer the path with the shortest AS_PATH.
        if (!this.bestAttrs.getAsPath().equals(attrs.getAsPath())) {
            // FIXME: this is something we can cache...
            final int bap = countAsPath(this.bestAttrs.getAsPath().getSegments());
            final int nap = countAsPath(attrs.getAsPath().getSegments());
            if (bap > nap) {
                return true;
            }
        }

        // 5. prefer the path with the lowest origin type
        // - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
        if (!this.bestAttrs.getOrigin().equals(attrs.getOrigin())) {
            final BgpOrigin bo = this.bestAttrs.getOrigin().getValue();
            final BgpOrigin no = attrs.getOrigin().getValue();

            // This trick relies on the order in which the values are declared in the model.
            if (no.ordinal() < bo.ordinal()) {
                return true;
            }
        }

        // FIXME: we should be able to cache the best AS
        final AsNumber bestAs = getPeerAs(this.bestAttrs.getAsPath().getSegments());
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
            if (this.bestAttrs.getMultiExitDisc() != null || attrs.getMultiExitDisc() != null) {
                final Long bmed = this.bestAttrs.getMultiExitDisc().getMed();
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
            if (!this.ourAs.equals(bestAs) && this.ourAs.equals(newAs)) {
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

        /*
         * RFC5004 states that this algorithm should end here and select existing path over new path in the
         * best path selection process. Benefits are listed in the RFC: @see http://tools.ietf.org/html/rfc500
         * - This algorithm SHOULD NOT be applied when either path is from a BGP Confederation peer.
         *  - not applicable, we don't deal with confederation peers
         * - The algorithm SHOULD NOT be applied when both paths are from peers with an identical BGP identifier
         *   (i.e., there exist parallel BGP sessions between two BGP speakers).
         *  - not applicable, BUG-2631 prevents parallel sessions to be created.
         */
        return true;
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
