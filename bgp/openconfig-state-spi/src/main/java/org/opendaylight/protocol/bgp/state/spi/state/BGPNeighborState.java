/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.spi.state;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState.Mode;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;

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
public interface BGPNeighborState {
    /**
     * Return Neighbor Key/Address
     *
     * @return neighbor Address
     */
    IpAddress getNeighborAddress();

    /**
     * @return Capabilities State
     */
    @Nonnull
    NeighborStateAugmentation getCapabilitiesState();

    /**
     * Counters for BGP messages (UPDATE / NOTIFICATION) sent and received from the neighbor
     *
     * @return Message state
     */
    @Nonnull
    BgpNeighborStateAugmentation getMessagesState();

    /**
     * Operational state parameters relating to BGP timers associated with the BGP session
     *
     * @return Timer State
     */
    @Nonnull
    NeighborTimersStateAugmentation getTimersState();

    /**
     * Operational state parameters relating to the transport session used for the BGP session
     *
     * @return Transport State
     */
    @Nonnull
    NeighborTransportStateAugmentation getTransportState();

    /**
     * Operational state parameters relating to enhanced error handling for BGP
     *
     * @return Error Handling State
     */
    @Nonnull
    NeighborErrorHandlingStateAugmentation getErrorHandlingState();

    /**
     * Operational state information relevant to graceful restart for BGP
     *
     * @return graceful restart State
     */
    @Nonnull
    NeighborGracefulRestartStateAugmentation getGracefulRestartState();

    /**
     * Operational state on a per-AFI-SAFI basis for a BGP neighbor
     *
     * @return Afi Safis States
     */
    @Nonnull
    List<AfiSafi> getAfisSafisState();

    /**
     * Paths installed under Effective-Rib-In for a BGP neighbor
     * Represented per Prefixes, the cost of calculate paths per each Prefix on Effective-Rib-in is not worth
     * at this point, check comment under incrementPrefixesInstalled
     * @return Paths counter
     */
    default long getTotalPaths() {
        return getTotalPrefixes();
    }

    /**
     * Prefixes installed under Effective-Rib-In for a BGP neighbor
     *
     * @return Paths counter
     */
    long getTotalPrefixes();

    /**
     * Set Capabilities for for a BGP neighbor session
     *
     * @param holdTimerValue hold timer
     * @param localPort local port
     * @param remoteAddress remote address
     * @param remotePort remote port
     * @param addPath additinional path capability
     * @param asn32 asn 4 bytes
     * @param gracefulRestart graceful restart capability
     * @param multiProtocol multiprotocol capability
     * @param routerRefresh router refresh capability
     */
    void setCapabilities(int holdTimerValue, @Nonnull PortNumber localPort, @Nonnull IpAddress remoteAddress,
        @Nonnull PortNumber remotePort, boolean addPath, boolean asn32, boolean gracefulRestart, boolean multiProtocol,
        boolean routerRefresh);

    /**
     * Set Graceful Restart config for a BGP neighbor
     *
     * @param peerRestartTime restart time
     * @param peerRestarting restarting
     * @param localRestarting local restarting
     * @param mode GracefulRestartState Mode
     */
    void setAfiSafiGracefulRestartState(int peerRestartTime, boolean peerRestarting, boolean localRestarting, Mode mode);

    /**
     * Advertized per Afi Safi Capabilities
     *
     * @param receivedAfiSafis received Afi Safis
     * @param receivedAfiSafisGraceful received Graceful Restart Afi Safis
     */
    void setActiveAfiSafi(@Nonnull Set<Class<? extends AfiSafiType>> receivedAfiSafis,
        @Nonnull Set<Class<? extends AfiSafiType>> receivedAfiSafisGraceful);

    /**
     * Increments Prefixed installed (Effective-rib-in)
     * Its only valid for non Additional path, when additional path supported this wont be
     * correct representation, since the cost for differentiate whether is a new prefix or
     * and existing one will mean to check all routes key per each new route installation.
     * @param afiSafiType afi Safi Type
     */
    void incrementPrefixesInstalled(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Decrement Prefixed installed (Effective-rib-in)
     *
     * @param afiSafiType afi Safi Type
     */
    void decrementPrefixesInstalled(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Increments Prefixed sent
     *
     * @param afiSafiType afi Safi Type
     */
    UnsignedInt32Counter getPrefixesSentCounter(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Increments Prefixed received
     *
     * @param afiSafiType afi Safi Type
     */
    void incrementPrefixesReceived(@Nonnull Class<? extends AfiSafiType> afiSafiType);

    /**
     * Increments Erroneous Update Received
     */
    void incrementErroneousUpdateReceived();

    /**
     * Increments Notification Messages Sent
     */
    void incrementNotificationSent();

    /**
     * Increments Update Messages Sent
     */
    void incrementUpdateSent();

    /**
     * Increments Update Messages Received
     */
    void incrementUpdateReceived();

    /**
     * Increments Notification Messages Received
     */
    void incrementNotificationReceived();


    /**
     * Operational state of the BGP session
     *
     * @param sessionState session State
     */
    void setState(@Nonnull SessionState sessionState);
}
