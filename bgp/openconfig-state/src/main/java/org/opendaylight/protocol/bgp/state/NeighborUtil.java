/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPLlGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiKey;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.Received;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.Sent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Util for create OpenConfig Neighbor with corresponding openConfig state.
 */
public final class NeighborUtil {
    private static final long TIMETICK_ROLLOVER_VALUE = UnsignedInteger.MAX_VALUE.longValue() + 1;

    private NeighborUtil() {
        // Hidden on purpose
    }

    /**
     * Build a Openconfig Neighbors container with all Neighbors Stats from a list of
     * BGPPeerGroupState.
     *
     * @param peerStats            List of BGPPeerState containing Neighbor state counters
     * @param bgpTableTypeRegistry BGP TableType Registry
     * @return Openconfig Neighbors Stats
     */
    public static @Nullable Neighbors buildNeighbors(final @NonNull List<BGPPeerState> peerStats,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        if (peerStats.isEmpty()) {
            return null;
        }
        return new NeighborsBuilder().setNeighbor(peerStats.stream()
                .filter(Objects::nonNull)
                .map(neighbor -> buildNeighbor(neighbor, bgpTableTypeRegistry))
                .collect(BindingMap.toMap())).build();
    }

    /**
     * Build a list of neighbors containing Operational State from a list of BGPPeerState.
     *
     * @param neighbor containing Neighbor state counters
     * @return neighbor containing Neighbor State
     */
    public static @NonNull Neighbor buildNeighbor(final @NonNull BGPPeerState neighbor,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new NeighborBuilder()
                .setNeighborAddress(convertIpAddress(neighbor.getNeighborAddress()))
                .setState(buildNeighborState(neighbor.getBGPSessionState(), neighbor.getBGPPeerMessagesState()))
                .setTimers(buildTimer(neighbor.getBGPTimersState()))
                .setTransport(buildTransport(neighbor.getBGPTransportState()))
                .setErrorHandling(buildErrorHandling(neighbor.getBGPErrorHandlingState()))
                .setGracefulRestart(buildGracefulRestart(neighbor.getBGPGracelfulRestart()))
                .setAfiSafis(buildAfisSafis(neighbor, bgpTableTypeRegistry))
                .build();
    }

    private static IpAddress convertIpAddress(final IpAddressNoZone addr) {
        if (addr == null) {
            return null;
        }
        final Ipv4AddressNoZone ipv4 = addr.getIpv4AddressNoZone();
        if (ipv4 != null) {
            return new IpAddress(ipv4);
        }
        return new IpAddress(addr.getIpv6AddressNoZone());
    }

    /**
     * Builds Neighbor State from BGPPeerState counters.
     *
     * @param sessionState         BGPPeerState containing Operational state counters
     * @param bgpPeerMessagesState message state
     * @return Neighbor State
     */
    public static @Nullable State buildNeighborState(final @Nullable BGPSessionState sessionState,
            final BGPPeerMessagesState bgpPeerMessagesState) {
        if (sessionState == null && bgpPeerMessagesState == null) {
            return null;
        }
        final StateBuilder builder = new StateBuilder();
        if (sessionState != null) {
            builder.addAugmentation(buildCapabilityState(sessionState));
        }
        if (bgpPeerMessagesState != null) {
            builder.addAugmentation(buildMessageState(bgpPeerMessagesState));
        }
        return builder.build();
    }

    /**
     * Builds Neighbor State from BGPPeerState counters.
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Timer State
     */
    public static @Nullable Timers buildTimer(final @Nullable BGPTimersState neighbor) {
        if (neighbor == null) {
            return null;
        }
        // convert neighbor uptime which is in milliseconds to time-ticks which is
        // hundredth of a second, and handle roll-over scenario
        final long uptimeTicks = neighbor.getUpTime() / 10 % TIMETICK_ROLLOVER_VALUE;

        return new TimersBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .timers.StateBuilder()
                        .addAugmentation(new NeighborTimersStateAugmentationBuilder()
                            .setNegotiatedHoldTime(Decimal64.valueOf(neighbor.getNegotiatedHoldTime()))
                            .setUptime(new Timeticks(Uint32.valueOf(uptimeTicks))).build())
                        .build())
                .build();
    }

    /**
     * Builds Transport State from BGPTransportState counters.
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Transport State
     */
    public static @Nullable Transport buildTransport(final @Nullable BGPTransportState neighbor) {
        if (neighbor == null) {
            return null;
        }

        return new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
                .bgp.neighbor.group.transport.StateBuilder()
                    .addAugmentation(new NeighborTransportStateAugmentationBuilder()
                        .setLocalPort(neighbor.getLocalPort())
                        .setRemoteAddress(convertIpAddress(neighbor.getRemoteAddress()))
                        .setRemotePort(neighbor.getRemotePort())
                        .build())
                    .build())
                .build();
    }

    /**
     * Builds Error Handling State from BGPPeerState counters.
     *
     * @param errorHandlingState BGPErrorHandlingState containing ErrorHandlingState Operational state counters
     * @return Error Handling State
     */
    public static ErrorHandling buildErrorHandling(final @Nullable BGPErrorHandlingState errorHandlingState) {
        if (errorHandlingState == null) {
            return null;
        }
        return new ErrorHandlingBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .error.handling.StateBuilder()
                        .addAugmentation(buildErrorHandlingState(errorHandlingState.getErroneousUpdateReceivedCount()))
                        .build())
                .build();
    }

    /**
     * Builds Graceful Restart containing Graceful Restart State from BGPGracelfulRestartState counters.
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Graceful Restart
     */
    public static @NonNull GracefulRestart buildGracefulRestart(final @NonNull BGPGracelfulRestartState neighbor) {
        final NeighborGracefulRestartStateAugmentation gracefulRestartState =
                new NeighborGracefulRestartStateAugmentationBuilder()
                        .setLocalRestarting(neighbor.isLocalRestarting())
                        .setPeerRestartTime(Uint16.valueOf(neighbor.getPeerRestartTime()))
                        .setMode(neighbor.getMode())
                        .setPeerRestarting(neighbor.isPeerRestarting()).build();

        return new GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
                .addAugmentation(gracefulRestartState).build()).build();
    }

    /**
     * Builds  Neighbor Afi Safi containing AfiSafi State.
     *
     * @param neighbor BGPPeerState containing Operational state counters
     * @return Afi Safis
     */
    public static AfiSafis buildAfisSafis(final @NonNull BGPPeerState neighbor,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        return new AfiSafisBuilder().setAfiSafi(buildAfisSafisState(neighbor.getBGPAfiSafiState(),
                bgpTableTypeRegistry)).build();
    }

    /**
     * Builds  Neighbor State containing Capabilities State, session State.
     *
     * @return Neighbor State
     */
    public static NeighborStateAugmentation buildCapabilityState(final @NonNull BGPSessionState neighbor) {

        final Set<Class<? extends BgpCapability>> supportedCapabilities = buildSupportedCapabilities(neighbor);
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
     * Builds Bgp Neighbor State containing Message State.
     *
     * @return BgpNeighborState containing Message State
     */
    public static @NonNull BgpNeighborStateAugmentation buildMessageState(
            final @NonNull BGPPeerMessagesState neighbor) {
        return new BgpNeighborStateAugmentationBuilder()
                .setMessages(new MessagesBuilder()
                        .setReceived(buildMessagesReceived(neighbor))
                        .setSent(buildMessagesSent(neighbor)).build()).build();
    }

    private static Received buildMessagesReceived(final @NonNull BGPPeerMessagesState neighbor) {
        return new ReceivedBuilder()
                .setUPDATE(Uint64.valueOf(neighbor.getUpdateMessagesReceivedCount()))
                .setNOTIFICATION(Uint64.valueOf(neighbor.getNotificationMessagesReceivedCount()))
                .build();
    }

    private static Sent buildMessagesSent(final @NonNull BGPPeerMessagesState neighbor) {
        return new SentBuilder()
                .setUPDATE(Uint64.valueOf(neighbor.getUpdateMessagesSentCount()))
                .setNOTIFICATION(Uint64.valueOf(neighbor.getNotificationMessagesSentCount()))
                .build();
    }

    /**
     * Builds Neighbor Error Handling State.
     *
     * @param erroneousUpdateCount erroneous Update Count
     * @return Error Handling State
     */
    public static @NonNull NeighborErrorHandlingStateAugmentation buildErrorHandlingState(
            final long erroneousUpdateCount) {
        return new NeighborErrorHandlingStateAugmentationBuilder()
                .setErroneousUpdateMessages(saturatedUint32(erroneousUpdateCount)).build();
    }

    /**
     * Build List of afi safi containing State per Afi Safi.
     *
     * @return AfiSafi List
     */
    public static @NonNull Map<AfiSafiKey, AfiSafi> buildAfisSafisState(final @NonNull BGPAfiSafiState neighbor,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Set<TablesKey> afiSafiJoin = new HashSet<>(neighbor.getAfiSafisAdvertized());
        afiSafiJoin.addAll(neighbor.getAfiSafisReceived());
        return afiSafiJoin.stream().map(tableKey -> buildAfiSafi(neighbor, tableKey, bgpTableTypeRegistry))
                .filter(Objects::nonNull)
                .collect(BindingMap.toMap());
    }

    private static @Nullable AfiSafi buildAfiSafi(final @NonNull BGPAfiSafiState neighbor,
            final @NonNull TablesKey tablesKey, final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry) {
        final Class<? extends AfiSafiType> afiSafi = bgpTableTypeRegistry.getAfiSafiType(tablesKey);
        return afiSafi == null ? null : new AfiSafiBuilder()
            .setAfiSafiName(afiSafi)
            .setState(buildAfiSafiState(neighbor, tablesKey, neighbor.isAfiSafiSupported(tablesKey)))
            .setGracefulRestart(buildAfiSafiGracefulRestartState(neighbor, tablesKey))
            .build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
            .safi.list.afi.safi.State buildAfiSafiState(final @NonNull BGPAfiSafiState neighbor,
            final @NonNull TablesKey tablesKey, final boolean afiSafiSupported) {
        final NeighborAfiSafiStateAugmentationBuilder builder = new NeighborAfiSafiStateAugmentationBuilder();
        builder.setActive(afiSafiSupported);
        if (afiSafiSupported) {
            builder.setPrefixes(new PrefixesBuilder()
                    .setInstalled(saturatedUint32(neighbor.getPrefixesInstalledCount(tablesKey)))
                    .setReceived(saturatedUint32(neighbor.getPrefixesReceivedCount(tablesKey)))
                    .setSent(saturatedUint32(neighbor.getPrefixesSentCount(tablesKey))).build());
        }
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
                .safi.list.afi.safi.StateBuilder().addAugmentation(builder.build()).build();
    }

    // FIXME: remove this with YANGTOOLS-5.0.7+
    private static Uint32 saturatedUint32(final long value) {
        return value < 4294967295L ? Uint32.valueOf(value) : Uint32.MAX_VALUE;
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
            .safi.list.afi.safi.GracefulRestart buildAfiSafiGracefulRestartState(
            final @NonNull BGPLlGracelfulRestartState neighbor, final @NonNull TablesKey tablesKey) {
        final NeighborAfiSafiGracefulRestartStateAugmentation builder =
                new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                        .setAdvertised(neighbor.isGracefulRestartAdvertized(tablesKey))
                        .setReceived(neighbor.isGracefulRestartReceived(tablesKey))
                        .setLlAdvertised(neighbor.isLlGracefulRestartAdvertised(tablesKey))
                        .setLlReceived(neighbor.isLlGracefulRestartReceived(tablesKey))
                        .setLlStaleTimer(Uint32.valueOf(neighbor.getLlGracefulRestartTimer(tablesKey))).build();
        return new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi
                .safi.list.afi.safi.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig
                .net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder()
                .addAugmentation(builder).build()).build();
    }

    /**
     * Builds List of BgpCapability supported capabilities.
     *
     * @return List containing supported capabilities
     */
    public static @NonNull Set<Class<? extends BgpCapability>> buildSupportedCapabilities(
            final @NonNull BGPSessionState neighbor) {
        final var supportedCapabilities = ImmutableSet.<Class<? extends BgpCapability>>builder();
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
        return supportedCapabilities.build();
    }
}
