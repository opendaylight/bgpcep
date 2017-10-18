/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

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
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Received;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Sent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;

/**
 * Util for create OpenConfig Neighbor with corresponding openConfig state.
 */
public final class NeighborUtil {
    private NeighborUtil() {
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
            .filter(Objects::nonNull)
            .map(neighbor -> buildNeighbor(neighbor, bgpTableTypeRegistry))
            .collect(Collectors.toList())).build();
    }

    /**
     * Build a list of neighbors containing Operational State from a list of BGPPeerState
     *
     * @param neighbor containing Neighbor state counters
     * @return neighbor containing Neighbor State
     */
    @Nullable
    public static Neighbor buildNeighbor(@Nonnull final BGPPeerState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new NeighborBuilder()
            .setNeighborAddress(neighbor.getNeighborAddress())
            .setState(buildNeighborState(neighbor.getBGPSessionState(), neighbor.getBGPPeerMessagesState()))
            .setTimers(buildTimer(neighbor.getBGPTimersState()))
            .setTransport(buildTransport(neighbor.getBGPTransportState()))
            .setErrorHandling(buildErrorHandling(neighbor.getBGPErrorHandlingState()))
            .setGracefulRestart(buildGracefulRestart(neighbor.getBGPGracelfulRestart()))
            .setAfiSafis(buildAfisSafis(neighbor, bgpTableTypeRegistry))
            .build();
    }

    /**
     * Builds Neighbor State from BGPPeerState counters
     *
     * @param sessionState BGPPeerState containing Operational state counters
     * @param bgpPeerMessagesState
     * @return Neighbor State
     */
    @Nullable
    public static State buildNeighborState(@Nullable final BGPSessionState sessionState,
        final BGPPeerMessagesState bgpPeerMessagesState) {
        if (sessionState == null && bgpPeerMessagesState == null) {
            return null;
        }
        final StateBuilder builder = new StateBuilder();
        if (sessionState != null) {
            builder.addAugmentation(NeighborStateAugmentation.class, buildCapabilityState(sessionState));
        }
        if (bgpPeerMessagesState != null) {
            builder.addAugmentation(BgpNeighborStateAugmentation.class, buildMessageState(bgpPeerMessagesState));
        }
        return builder.build();
    }

    /**
     * Builds Neighbor State from BGPPeerState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Timer State
     */
    @Nullable
    public static Timers buildTimer(@Nullable final BGPTimersState neighbor) {
        if (neighbor == null) {
            return null;
        }
        final NeighborTimersStateAugmentation timerState = new NeighborTimersStateAugmentationBuilder()
            .setNegotiatedHoldTime(BigDecimal.valueOf(neighbor.getNegotiatedHoldTime()))
            .setUptime(new Timeticks(neighbor.getUpTime())).build();

        return new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
            .neighbor.group.timers.StateBuilder()
            .addAugmentation(NeighborTimersStateAugmentation.class, timerState).build()).build();
    }

    /**
     * Builds Transport State from BGPTransportState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Transport State
     */
    @Nullable
    public static Transport buildTransport(@Nullable final BGPTransportState neighbor) {
        if (neighbor == null) {
            return null;
        }
        final NeighborTransportStateAugmentation transportState = new NeighborTransportStateAugmentationBuilder()
            .setLocalPort(neighbor.getLocalPort()).setRemoteAddress(neighbor.getRemoteAddress())
            .setRemotePort(neighbor.getRemotePort()).build();

        return new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
            .bgp.neighbor.group.transport.StateBuilder()
            .addAugmentation(NeighborTransportStateAugmentation.class, transportState).build()).build();
    }

    /**
     * Builds Error Handling State from BGPPeerState counters
     *
     * @param errorHandlingState BGPErrorHandlingState containing ErrorHandlingState Operational state counters
     * @return Error Handling State
     */
    public static ErrorHandling buildErrorHandling(@Nullable final BGPErrorHandlingState errorHandlingState) {
        if (errorHandlingState == null) {
            return null;
        }
        return new ErrorHandlingBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.
            rev151009.bgp.neighbor.group.error.handling.StateBuilder()
            .addAugmentation(NeighborErrorHandlingStateAugmentation.class,
                buildErrorHandlingState(errorHandlingState.getErroneousUpdateReceivedCount())).build()).build();
    }

    /**
     * Builds Graceful Restart containing Graceful Restart State from BGPGracelfulRestartState counters
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Graceful Restart
     */
    @Nonnull
    public static GracefulRestart buildGracefulRestart(@Nonnull final BGPGracelfulRestartState neighbor) {
        final NeighborGracefulRestartStateAugmentation gracefulRestartState =
            new NeighborGracefulRestartStateAugmentationBuilder()
            .setLocalRestarting(neighbor.isLocalRestarting())
            .setPeerRestartTime(neighbor.getPeerRestartTime())
            //.setMode(mode) TBD once implemented
            .setPeerRestarting(neighbor.isPeerRestarting()).build();

        return new GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
            .addAugmentation(NeighborGracefulRestartStateAugmentation.class, gracefulRestartState).build()).build();
    }

    /**
     * Builds  Neighbor Afi Safi containing AfiSafi State
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Afi Safis
     */
    public static AfiSafis buildAfisSafis(@Nonnull final BGPPeerState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new AfiSafisBuilder().setAfiSafi(buildAfisSafisState(neighbor.getBGPAfiSafiState(), bgpTableTypeRegistry)).build();
    }

    /**
     * Builds  Neighbor State containing Capabilities State, session State
     *
     * @return Neighbor State
     */
    public static NeighborStateAugmentation buildCapabilityState(@Nonnull final BGPSessionState neighbor) {

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
    public static BgpNeighborStateAugmentation buildMessageState(@Nonnull final BGPPeerMessagesState neighbor) {
        return new BgpNeighborStateAugmentationBuilder()
            .setMessages(new MessagesBuilder()
                .setReceived(buildMessagesReceived(neighbor))
                .setSent(buildMessagesSent(neighbor)).build()).build();
    }

    private static Received buildMessagesReceived(@Nonnull final BGPPeerMessagesState neighbor) {
        return new ReceivedBuilder()
            .setUPDATE(toBigInteger(neighbor.getUpdateMessagesReceivedCount()))
            .setNOTIFICATION(toBigInteger(neighbor.getNotificationMessagesReceivedCount()))
            .build();
    }

    public static BigInteger toBigInteger(final long updateReceivedCounter) {
        return UnsignedLong.valueOf(updateReceivedCounter).bigIntegerValue();
    }

    private static Sent buildMessagesSent(@Nonnull final BGPPeerMessagesState neighbor) {
        return new SentBuilder()
            .setUPDATE(toBigInteger(neighbor.getUpdateMessagesSentCount()))
            .setNOTIFICATION(toBigInteger(neighbor.getNotificationMessagesSentCount()))
            .build();
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
    public static List<AfiSafi> buildAfisSafisState(@Nonnull final BGPAfiSafiState neighbor,
        @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Set<TablesKey> afiSafiJoin = new HashSet<>(neighbor.getAfiSafisAdvertized());
        afiSafiJoin.addAll(neighbor.getAfiSafisReceived());
        return afiSafiJoin.stream().map(tableKey -> buildAfiSafi(neighbor, tableKey, bgpTableTypeRegistry))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static AfiSafi buildAfiSafi(@Nonnull final BGPAfiSafiState neighbor,
        @Nonnull final TablesKey tablesKey, @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Optional<Class<? extends AfiSafiType>> afiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
        if (!afiSafi.isPresent()) {
            return null;
        }

        return new AfiSafiBuilder().setAfiSafiName(afiSafi.get())
            .setState(buildAfiSafiState(neighbor, tablesKey, neighbor.isAfiSafiSupported(tablesKey)))
            .setGracefulRestart(buildAfiSafiGracefulRestartState(neighbor, tablesKey)).build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.
        safi.list.afi.safi.State buildAfiSafiState(@Nonnull final BGPAfiSafiState neighbor,
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
        .safi.list.afi.safi.GracefulRestart buildAfiSafiGracefulRestartState(@Nonnull final BGPGracelfulRestartState neighbor,
        @Nonnull final TablesKey tablesKey) {
        final NeighborAfiSafiGracefulRestartStateAugmentation builder =
            new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
            .setAdvertised(neighbor.isGracefulRestartAdvertized(tablesKey))
                .setReceived(neighbor.isGracefulRestartReceived(tablesKey)).build();
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
    public static List<Class<? extends BgpCapability>> buildSupportedCapabilities(@Nonnull final BGPSessionState neighbor) {
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
