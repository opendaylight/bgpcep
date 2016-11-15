/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandling;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandlingBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ASN32;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.GRACEFULRESTART;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.MPBGP;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ROUTEREFRESH;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Received;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Sent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

/**
 * Util for create OpenConfig Neighbor with corresponding openConfig state.
 */
public final class NeighborStateUtil {
    private NeighborStateUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Build a Openconfig Neighbors container with all Neighbors Stats from a list of
     * BGPPeerGroupState
     *
     * @param peerStats List of BGPPeerState containing Neighbor state counters
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return Openconfig Neighbors Stats
     */
    @Nullable
    public static Neighbors buildNeighbors(@Nonnull final List<BGPPeerState> peerStats,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        if (peerStats.isEmpty()) {
            return null;
        }
        return new NeighborsBuilder().setNeighbor(peerStats.stream()
            .map(neighbor -> buildNeighbor(neighbor, bgpTableTypeRegistry))
            .collect(Collectors.toList())).build();
    }

    /**
     * Build a list of neighbors containing Operational State from a list of BGPPeerState
     *
     * @param neighbor containing Neighbor state counters
     * @return neighbor containing Neighbor State
     */
    @Nonnull
    public static Neighbor buildNeighbor(@Nonnull final BGPPeerState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new NeighborBuilder()
            .setNeighborAddress(neighbor.getNeighborAddress())
            .setState(buildNeighborState(neighbor))
            .setTimers(buildTimer(neighbor))
            .setTransport(buildTransport(neighbor))
            .setErrorHandling(buildErrorHandling(neighbor))
            .setGracefulRestart(buildGracefulRestart(neighbor))
            .setAfiSafis(buildAfisSafis(neighbor, bgpTableTypeRegistry))
            .build();
    }

    /**
     * Builds Neighbor State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Neighbor State
     */
    @Nonnull
    public static State buildNeighborState(@Nonnull final BGPPeerState neighbor) {
        return new StateBuilder()
            .addAugmentation(NeighborStateAugmentation.class, buildCapabilityState(neighbor))
            .addAugmentation(BgpNeighborStateAugmentation.class,
                buildMessageState(neighbor)).build();
    }

    /**
     * Builds Neighbor State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Timer State
     */
    public static Timers buildTimer(@Nonnull final BGPPeerState neighbor) {
        return new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
            .neighbor.group.timers.StateBuilder()
            .addAugmentation(NeighborTimersStateAugmentation.class, buildTimerState(neighbor)).build()).build();
    }

    /**
     * Builds Transport State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Transport State
     */
    @Nonnull
    public static Transport buildTransport(@Nonnull final BGPPeerState neighbor) {
        return new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
            .bgp.neighbor.group.transport.StateBuilder()
            .addAugmentation(NeighborTransportStateAugmentation.class, buildTransportState(neighbor)).build()).build();
    }

    /**
     * Builds Error Handling State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Error Handling State
     */
    public static ErrorHandling buildErrorHandling(@Nonnull final BGPPeerState neighbor) {
        return new ErrorHandlingBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.
            rev151009.bgp.neighbor.group.error.handling.StateBuilder()
            .addAugmentation(NeighborErrorHandlingStateAugmentation.class,
                buildErrorHandlingState(neighbor.getErroneousUpdateReceivedCount())).build()).build();
    }

    /**
     * Builds Graceful Restart containing Graceful Restart State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Graceful Restart
     */
    @Nonnull
    public static GracefulRestart buildGracefulRestart(@Nonnull final BGPPeerState neighbor) {
        return new GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
            .addAugmentation(NeighborGracefulRestartStateAugmentation.class,
                buildGracefulRestartState(neighbor)).build()).build();
    }

    /**
     * Builds Graceful Restart State from BGPPeerState counters
     *
     * @return Graceful Restart State
     */
    public static NeighborGracefulRestartStateAugmentation buildGracefulRestartState(
        @Nonnull final BGPPeerState neighbor) {
        return new NeighborGracefulRestartStateAugmentationBuilder()
            .setLocalRestarting(neighbor.isLocalRestarting())
            .setPeerRestartTime(neighbor.isPeerRestartTime())
            //.setMode(mode) TBD once implemented
            .setPeerRestarting(neighbor.isPeerRestarting()).build();
    }

    /**
     * Builds  Neighbor Afi Safi containing AfiSafi State
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Afi Safis
     */
    public static AfiSafis buildAfisSafis(@Nonnull final BGPPeerState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new AfiSafisBuilder().setAfiSafi(buildAfisSafisState(neighbor, bgpTableTypeRegistry)).build();
    }

    /**
     * Builds  Neighbor State containing Capabilities State, session State
     *
     * @return Neighbor State
     */
    public static NeighborStateAugmentation buildCapabilityState(@Nonnull final BGPPeerState neighbor) {

        final List<Class<? extends BgpCapability>> supportedCapabilities = buildSupportedCapabilities(neighbor);
        SessionState sessionState = null;
        switch (neighbor.getSessionState()) {
        case IDLE:
            sessionState = SessionState.IDLE;
            break;
        case UP:
            sessionState = SessionState.ESTABLISHED;
            break;
        case OPEN_CONFIRM:
            sessionState = SessionState.OPENCONFIRM;
            break;
        default:
        }
        return new NeighborStateAugmentationBuilder().setSupportedCapabilities(supportedCapabilities)
            .setSessionState(sessionState).build();
    }

    /**
     * Builds Bgp Neighbor State containing Message State
     *
     * @return BgpNeighborState containing Message State
     */
    @Nonnull
    public static BgpNeighborStateAugmentation buildMessageState(@Nonnull final BGPPeerState neighbor) {
        return new BgpNeighborStateAugmentationBuilder()
            .setMessages(new MessagesBuilder()
                .setReceived(buildMessagesReceived(neighbor))
                .setSent(buildMessagesSent(neighbor)).build()).build();
    }

    private static Received buildMessagesReceived(@Nonnull final BGPPeerState neighbor) {
        return new ReceivedBuilder()
            .setUPDATE(toBigInteger(neighbor.getUpdateMessagesReceivedCount()))
            .setNOTIFICATION(toBigInteger(neighbor.getNotificationMessagesReceivedCount()))
            .build();
    }

    public static BigInteger toBigInteger(final long updateReceivedCounter) {
        return UnsignedLong.valueOf(updateReceivedCounter).bigIntegerValue();
    }

    private static Sent buildMessagesSent(@Nonnull final BGPPeerState neighbor) {
        return new SentBuilder()
            .setUPDATE(toBigInteger(neighbor.getUpdateMessagesSentCount()))
            .setNOTIFICATION(toBigInteger(neighbor.getNotificationMessagesSentCount()))
            .build();
    }

    /**
     * Builds Neighbor Timers State
     *
     * @return NeighborTimersState
     */
    @Nonnull
    public static NeighborTimersStateAugmentation buildTimerState(@Nonnull final BGPPeerState neighbor) {
        return new NeighborTimersStateAugmentationBuilder()
            .setNegotiatedHoldTime(BigDecimal.valueOf(neighbor.getNegotiatedHoldTime()))
            .setUptime(new Timeticks(neighbor.getUpTime())).build();
    }

    /**
     * Builds Transport State
     *
     * @return Transport State
     */
    @Nonnull
    public static NeighborTransportStateAugmentation buildTransportState(@Nonnull final BGPPeerState neighbor) {
        return new NeighborTransportStateAugmentationBuilder()
            .setLocalPort(neighbor.getLocalPort()).setRemoteAddress(neighbor.getRemoteAddress())
            .setRemotePort(neighbor.getRemotePort()).build();
    }

    /**
     * Builds Neighbor Error Handling State
     *
     * @param erroneousUpdateCount erroneous Update Count
     * @return Error Handling State
     */
    @Nonnull
    public static NeighborErrorHandlingStateAugmentation buildErrorHandlingState(final long erroneousUpdateCount) {
        return new NeighborErrorHandlingStateAugmentationBuilder()
            .setErroneousUpdateMessages(erroneousUpdateCount).build();
    }

    /**
     * Build List of afi safi containing State per Afi Safi
     *
     * @return AfiSafi List
     */
    @Nonnull
    public static List<AfiSafi> buildAfisSafisState(@Nonnull final BGPPeerState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Set<TablesKey> afiSafiJoin = new HashSet<>(neighbor.getAfiSafisAdvertized());
        afiSafiJoin.addAll(neighbor.getAfiSafisReceived());
        return afiSafiJoin.stream().map(tableKey -> buildAfiSafi(neighbor, tableKey, bgpTableTypeRegistry))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static AfiSafi buildAfiSafi(@Nonnull final BGPPeerState neighbor,
        @Nonnull final TablesKey tablesKey, @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Optional<Class<? extends AfiSafiType>> afiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
        if (afiSafi.isPresent()) {
            return null;
        }

        return new AfiSafiBuilder().setAfiSafiName(afiSafi.get())
            .setState(buildAfiSafiState(neighbor, tablesKey, neighbor.isAfiSafiSupported(tablesKey)))
            .setGracefulRestart(buildAfiSafiGracefulRestartState(neighbor.isGracefulRestartAdvertized(tablesKey),
                neighbor.isGracefulRestartReceived(tablesKey))).build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.
        safi.list.afi.safi.State buildAfiSafiState(@Nonnull final BGPPeerState neighbor,
        @Nonnull final TablesKey tablesKey, final boolean afiSafiSupported) {
        final NeighborAfiSafiStateAugmentationBuilder builder = new NeighborAfiSafiStateAugmentationBuilder();
        builder.setActive(afiSafiSupported);
        if (afiSafiSupported) {
            builder.setPrefixes(new PrefixesBuilder()
                .setInstalled(neighbor.getPrefixesInstalledCount(tablesKey))
                .setReceived(neighbor.getPrefixesReceivedCount(tablesKey))
                .setSent(neighbor.getPrefixesSentCount(tablesKey)).build());
        }
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
            .safi.list.afi.safi.StateBuilder().addAugmentation(NeighborAfiSafiStateAugmentation.class, builder.build()).build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
        .safi.list.afi.safi.GracefulRestart buildAfiSafiGracefulRestartState(final boolean gracefulRestartAdvertized,
        final boolean gracefulRestartReceived) {
        final NeighborAfiSafiGracefulRestartStateAugmentation builder = new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
            .setAdvertised(gracefulRestartAdvertized).setReceived(gracefulRestartReceived).build();
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
            .safi.list.afi.safi.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig
            .net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder()
            .addAugmentation(NeighborAfiSafiGracefulRestartStateAugmentation.class, builder).build()).build();
    }

    /**
     * Builds List of BgpCapability supported capabilities
     *
     * @return List containing supported capabilities
     */
    @Nonnull
    public static List<Class<? extends BgpCapability>> buildSupportedCapabilities(@Nonnull final BGPPeerState neighbor) {
        final List<Class<? extends BgpCapability>> supportedCapabilities = new ArrayList<>();
        if (neighbor.isAddPathCapabilitySupported()) {
            supportedCapabilities.add(ADDPATHS.class);
        }
        if (neighbor.isAsn32CapabilitySupported()) {
            supportedCapabilities.add(ASN32.class);
        }
        if (neighbor.isGracefulRestartCapabilitySupported()) {
            supportedCapabilities.add(GRACEFULRESTART.class);
        }
        if (neighbor.isMultiProtocolCapabilitySupported()) {
            supportedCapabilities.add(MPBGP.class);
        }
        if (neighbor.isRouterRefreshCapabilitySupported()) {
            supportedCapabilities.add(ROUTEREFRESH.class);
        }
        return supportedCapabilities;
    }
}
