/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.rib.spi.RouterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.Uint32;

public class AbstractBestPathSelector {
    private final long ourAs;
    protected RouterId bestOriginatorId = null;
    protected BestPathState bestState = null;

    protected AbstractBestPathSelector(final long ourAs) {
        this.ourAs = ourAs;
    }

    /**
     * RFC 4456 mandates the use of Originator IDs instead of Router ID for
     * selection purposes.
     *
     * @param routerId     routerID
     * @param originatorId originator
     * @return returns originators Id if present otherwise routerId
     */
    protected RouterId replaceOriginator(final RouterId routerId, final OriginatorId originatorId) {
        return originatorId != null ? RouterId.forAddress(originatorId.getOriginator()) : routerId;
    }

    /**
     * Chooses best route according to BGP best path selection.
     *
     * @param state attributes of the new route
     * @return true if the existing path is better, false if the new path is better
     */
    protected boolean isExistingPathBetter(final @NonNull BestPathState state) {
        // 0. draft-uttaro-idr-bgp-persistence-04 defines "depreferenced" paths
        final boolean stateDepref = state.isDepreferenced();
        if (this.bestState.isDepreferenced() != stateDepref) {
            return stateDepref;
        }

        // 1. prefer path with accessible nexthop
        // - we assume that all nexthops are accessible
        /*
         * 2. prefer path with higher LOCAL_PREF
         *
         * FIXME: for eBGP cases (when the LOCAL_PREF is missing), we should assign a policy-based preference
         *        before we ever get here.
         */
        final Uint32 bestLocal = this.bestState.getLocalPref();
        final Uint32 stateLocal = state.getLocalPref();
        if (stateLocal != null) {
            if (bestLocal == null) {
                return true;
            }

            final int cmp = stateLocal.compareTo(bestLocal);
            if (cmp != 0) {
                return cmp < 0;
            }
        } else if (bestLocal != null) {
            return false;
        }

        // 3. prefer learned path
        // - we assume that all paths are learned

        // 4. prefer the path with the shortest AS_PATH.
        if (this.bestState.getAsPathLength() != state.getAsPathLength()) {
            return this.bestState.getAsPathLength() < state.getAsPathLength();
        }

        // 5. prefer the path with the lowest origin type
        // - IGP is lower than Exterior Gateway Protocol (EGP), and EGP is lower than INCOMPLETE
        if (!this.bestState.getOrigin().equals(state.getOrigin())) {
            final BgpOrigin bo = this.bestState.getOrigin();
            final BgpOrigin no = state.getOrigin();

            // This trick relies on the order in which the values are declared in the model.
            return no.ordinal() > bo.ordinal();
        }
        // FIXME: we should be able to cache the best AS
        final long bestAs = this.bestState.getPeerAs();
        final long newAs = state.getPeerAs();

        /*
         * Checks 6 and 7 are mutually-exclusive, as MEDs are comparable
         * only when the routes originated from the same AS. On the other
         * hand, when they are from the same AS, they are in the same iBGP/eBGP
         * relationship.
         *
         */
        if (bestAs == newAs) {
            // 6. prefer the path with the lowest multi-exit discriminator (MED)
            final Boolean cmp = firstLower(this.bestState.getMultiExitDisc(), state.getMultiExitDisc());
            if (cmp != null) {
                return cmp;
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
            if (this.ourAs != bestAs && this.ourAs == newAs) {
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
         * in the order of increasing router ID, but RFC-4456 Route Reflection throws a wrench into that.
         *
         * With RFC-5004, this gets a bit easier, because it completely eliminates step f) and later :-)
         *
         * RFC-5004 states that this algorithm should end here and select existing path over new path in the
         * best path selection process. Benefits are listed in the RFC: @see http://tools.ietf.org/html/rfc500
         * - This algorithm SHOULD NOT be applied when either path is from a BGP Confederation peer.
         *  - not applicable, we don't deal with confederation peers
         * - The algorithm SHOULD NOT be applied when both paths are from peers with an identical BGP identifier
         *   (i.e., there exist parallel BGP sessions between two BGP speakers).
         *  - not applicable, BUG-2631 prevents parallel sessions to be created.
         */
        return true;
    }

    private static Boolean firstLower(final long first, final long second) {
        return first < second ? Boolean.TRUE : first == second ? null : Boolean.FALSE;

    }
}
