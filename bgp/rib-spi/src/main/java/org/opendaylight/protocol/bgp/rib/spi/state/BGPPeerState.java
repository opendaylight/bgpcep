/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.state;

import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

/**
 * Representing operational state related to a particular BGP neighbor
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
     * PeerGroup Id
     *
     * @return PeerGroup Id
     */
    String getGroupId();

    /**
     * Return Neighbor Key/Address
     *
     * @return neighbor Address
     */
    IpAddress getNeighborAddress();

    /**
     * Paths installed under Effective-Rib-In for a BGP neighbor
     * Represented per Prefixes, the cost of calculate paths per each Prefix on Effective-Rib-in is not worth
     * at this point, check comment under incrementPrefixesInstalled
     *
     * @return Paths counter
     */
    default long getTotalPathsCount() {
        return getTotalPrefixes();
    }

    /**
     * Prefixes installed under Effective-Rib-In for a BGP neighbor
     *
     * @return Paths counter
     */
    long getTotalPrefixes();

    /**
     * Prefixed sent count
     *
     * @param tablesKey tablesKey Type
     * @return Prefixes sent count
     */
    long getPrefixesSentCount(@Nonnull TablesKey tablesKey);

    /**
     * Prefixed received count
     *
     * @param tablesKey tablesKey Type
     * @return Prefixed received count
     */
    long getPrefixesReceivedCount(@Nonnull TablesKey tablesKey);

    /**
     * Erroneous Update Received count
     *
     * @return count
     */
    long getErroneousUpdateReceivedCount();

    /**
     * Update Messages Sent count
     *
     * @return count
     */
    long getUpdateMessagesSentCount();

    /**
     * Notification Messages Sent count
     *
     * @return count
     */
    long getNotificationMessagesSentCount();

    /**
     * Update Messages Received count
     *
     * @return count
     */
    long getUpdateMessagesReceivedCount();

    /**
     * Notification Update Messages Received count
     *
     * @return count
     */
    long getNotificationMessagesReceivedCount();

    /**
     * Additional Path capability
     *
     * @return true if supported
     */
    boolean isAddPathCapabilitySupported();

    /**
     * AS 4 Bytes capability
     *
     * @return true if supported
     */
    boolean isAsn32CapabilitySupported();

    /**
     * Graceful Restart
     *
     * @return true if supported
     */
    boolean isGracefulRestartCapabilitySupported();

    /**
     * Multiprotocol capability
     *
     * @return true if supported
     */
    boolean isMultiProtocolCapabilitySupported();

    /**
     * Router Refresh Capability
     *
     * @return true if supported
     */
    boolean isRouterRefreshCapabilitySupported();

    /**
     * Internal session state
     *
     * @return Internal session state
     */
    State getSessionState();

    /**
     * List of TablesKey - Afi Safi Advertized to the neighbor
     *
     * @return TableKeys
     */
    @Nonnull
    Set<TablesKey> getAfiSafisAdvertized();

    /**
     * List of TablesKey - Afi Safi Advertized by neighbor
     *
     * @return TableKeys
     */
    @Nonnull
    Set<TablesKey> getAfiSafisReceived();

    /**
     * Prefixes installed per specific tablekey
     *
     * @param tablesKey tables Key
     * @return count
     */
    long getPrefixesInstalledCount(@Nonnull TablesKey tablesKey);

    /**
     * is AfiSafi Supported
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized to and by the neighbor
     */
    boolean isAfiSafiSupported(@Nonnull TablesKey tablesKey);

    /**
     * is Graceful Restart Supported advertized to neighbor
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized to neighbor
     */
    boolean isGracefulRestartAdvertized(@Nonnull TablesKey tablesKey);

    /**
     * is Graceful Restart Supported advertized by neighbor
     *
     * @param tablesKey tables Key
     * @return true if Afi Safi was advertized by neighbor
     */
    boolean isGracefulRestartReceived(TablesKey tablesKey);

    /**
     * Negotiated Hold Time
     *
     * @return Hold Time
     */
    long getNegotiatedHoldTime();

    /**
     * The time (MILLISECONDS) for how long session has been up or on idle state
     *
     * @return time
     */
    long getUpTime();

    /**
     * Local Port
     *
     * @return port
     */
    @Nonnull
    PortNumber getLocalPort();

    /**
     * Remote Address
     *
     * @return IpAddress
     */
    @Nonnull
    IpAddress getRemoteAddress();

    /**
     * Remote Port
     *
     * @return port
     */
    @Nonnull
    PortNumber getRemotePort();

    /**
     * This flag indicates whether the local neighbor is currently restarting
     *
     * @return local restarting state
     */
    boolean isLocalRestarting();

    /**
     * The period of time (advertised by the peer) that the peer expects a restart of a
     * BGP session to take
     *
     * @return time
     */
    int isPeerRestartTime();

    /**
     * This flag indicates whether the remote neighbor is currently in the process of
     * restarting, and hence received routes are currently stale
     *
     * @return peer is restarting
     */
    boolean isPeerRestarting();
}
