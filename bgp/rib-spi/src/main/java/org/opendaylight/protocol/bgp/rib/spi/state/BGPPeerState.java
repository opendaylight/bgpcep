/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Representing operational state related to a particular BGP neighbor.
 * - Counters for BGP messages sent and received from the neighbor
 * - Operational state of timers associated with the BGP neighbor
 * - Operational state of the transport session associated with the BGP neighbor
 * - Operational state of the error handling associated with the BGP neighbor
 * - Operational state of graceful-restart associated with a BGP neighbor
 * - Per-AFI-SAFI operational state and counters to the BGP neighbor
 * - Per-AFI-SAFI operational state for BGP graceful-restart
 */
public interface BGPPeerState extends RibReference {
    /**
     * Indicates whether this instance is being actively managed and updated.
     *
     * @return active
     */
    boolean isActive();

    /**
     * PeerGroup Id.
     *
     * @return PeerGroup Id
     */
    @Nullable
    String getGroupId();

    /**
     * Return Neighbor Key/Address.
     *
     * @return neighbor Address
     */
    @Nonnull
    IpAddress getNeighborAddress();

    /**
     * Paths installed under Effective-Rib-In for a BGP neighbor.
     * Represented per Prefixes, the cost of calculate paths per each Prefix on Effective-Rib-in is not worth
     * at this point, check comment under incrementPrefixesInstalled
     *
     * @return Paths counter
     */
    default long getTotalPathsCount() {
        return getTotalPrefixes();
    }

    /**
     * Prefixes installed under Effective-Rib-In for a BGP neighbor.
     *
     * @return Paths counter
     */
    long getTotalPrefixes();

    /**
     * Error Handling State.
     *
     * @return ErrorHandlingState
     */
    @Nonnull
    BGPErrorHandlingState getBGPErrorHandlingState();

    /**
     * Afi Safi Operational State.
     *
     * @return AfiSafiState
     */
    @Nonnull
    BGPAfiSafiState getBGPAfiSafiState();

    /**
     * BGP Session Operational State.
     *
     * @return BGPSessionState
     */
    @Nullable
    BGPSessionState getBGPSessionState();

    /**
     * BGP Message Operational State.
     *
     * @return BGPPeerMessagesState
     */
    @Nullable
    BGPPeerMessagesState getBGPPeerMessagesState();

    /**
     * BGP Operation Timers State.
     *
     * @return BGPTimersState
     */
    @Nullable
    BGPTimersState getBGPTimersState();

    /**
     * BGP Operational Transport State.
     *
     * @return BGPTransportState
     */
    @Nullable
    BGPTransportState getBGPTransportState();

    /**
     * BGP Operational GracelfulRestart State.
     *
     * @return BGPGracelfulRestartState
     */
    @Nonnull
    BGPGracelfulRestartState getBGPGracelfulRestart();
}
