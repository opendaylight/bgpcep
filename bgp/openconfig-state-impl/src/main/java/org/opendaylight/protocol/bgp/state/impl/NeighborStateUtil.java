/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.state.spi.counters.BGPCountersMessagesTypesCommon;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState.Mode;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ASN32;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.GRACEFULRESTART;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.MPBGP;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ROUTEREFRESH;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
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

/**
 * Util for create OpenConfig Neighbor with corresponding openConfig state.
 */
public final class NeighborStateUtil {
    private NeighborStateUtil() {
        throw new UnsupportedOperationException();
    }

    public static List<Neighbor> buildNeighbors(@Nonnull final List<BGPNeighborState> neighbors) {
        return neighbors.stream().map(NeighborStateUtil::buildNeighbor).collect(Collectors.toList());
    }

    public static Neighbor buildNeighbor(@Nonnull final BGPNeighborState neighbor) {
        return new NeighborBuilder()
            .setNeighborAddress(neighbor.getNeighborAddress())
            .setState(buildNeighborState(neighbor))
            .setTimers(buildTimer(neighbor))
            .setTransport(buildTransport(neighbor))
            .setErrorHandling(buildErrorHandling(neighbor))
            .setGracefulRestart(buildGracefulRestart(neighbor))
            .setAfiSafis(buildAfisSafis(neighbor))
            .build();
    }

    public static State buildNeighborState(@Nonnull final BGPNeighborState neighbor) {
        return new StateBuilder().addAugmentation(NeighborStateAugmentation.class, neighbor.getCapabilitiesState())
            .addAugmentation(BgpNeighborStateAugmentation.class, neighbor.getMessagesState()).build();
    }

    public static Timers buildTimer(@Nonnull final BGPNeighborState neighbor) {
        return new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp
            .neighbor.group.timers.StateBuilder()
            .addAugmentation(NeighborTimersStateAugmentation.class, neighbor.getTimersState()).build()).build();
    }

    public static Transport buildTransport(@Nonnull final BGPNeighborState neighbor) {
        return new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009
            .bgp.neighbor.group.transport.StateBuilder()
            .addAugmentation(NeighborTransportStateAugmentation.class, neighbor.getTransportState()).build()).build();
    }

    public static ErrorHandling buildErrorHandling(@Nonnull final BGPNeighborState neighbor) {
        return new ErrorHandlingBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.
            rev151009.bgp.neighbor.group.error.handling.StateBuilder()
            .addAugmentation(NeighborErrorHandlingStateAugmentation.class, neighbor.getErrorHandlingState()).build()).build();
    }

    public static GracefulRestart buildGracefulRestart(@Nonnull final BGPNeighborState neighbor) {
        return new GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
            .addAugmentation(NeighborGracefulRestartStateAugmentation.class, neighbor.getGracefulRestartState())
            .build()).build();
    }

    public static NeighborGracefulRestartStateAugmentation buildGracefulRestartState(
        final Boolean localRestarting, final Integer peerRestartTime, final Mode mode, final Boolean peerRestarting) {
        return new NeighborGracefulRestartStateAugmentationBuilder()
            .setLocalRestarting(localRestarting).setPeerRestartTime(peerRestartTime)
            .setMode(mode).setPeerRestarting(peerRestarting).build();
    }

    public static AfiSafis buildAfisSafis(@Nonnull final BGPNeighborState neighbor) {
        return new AfiSafisBuilder().setAfiSafi(neighbor.getAfisSafisState()).build();
    }

    public static NeighborStateAugmentation buildCapabilityState(
        @Nonnull final List<Class<? extends BgpCapability>> supportedCapabilities,
        final SessionState sessionState) {
        return new NeighborStateAugmentationBuilder().setSupportedCapabilities(supportedCapabilities)
            .setSessionState(sessionState).build();
    }

    public static BgpNeighborStateAugmentation buildMessageState(final BGPCountersMessagesTypesCommon receivedCounter,
        final BGPCountersMessagesTypesCommon sentCounter) {
        return new BgpNeighborStateAugmentationBuilder()
            .setMessages(new MessagesBuilder().setReceived(buildMessagesReceived(receivedCounter))
                .setSent(buildMessagesSent(sentCounter)).build()).build();
    }

    private static Received buildMessagesReceived(final BGPCountersMessagesTypesCommon receivedCounter) {
        return new ReceivedBuilder().setNOTIFICATION(receivedCounter.getNotificationCount())
            .setUPDATE(receivedCounter.getUpdateCount())
            .build();
    }

    private static Sent buildMessagesSent(final BGPCountersMessagesTypesCommon sentCounter) {
        return new SentBuilder().setNOTIFICATION(sentCounter.getNotificationCount())
            .setUPDATE(sentCounter.getUpdateCount()).build();
    }

    public static NeighborTimersStateAugmentation buildTimerState(final int holdTimerValue, final long elapsed) {
        return new NeighborTimersStateAugmentationBuilder().setNegotiatedHoldTime(BigDecimal.valueOf(holdTimerValue))
            .setUptime(new Timeticks(elapsed)).build();
    }

    public static NeighborTransportStateAugmentation buildTransportState(final int localPort,
        final IpAddress remoteAddress, final int remotePort) {
        return new NeighborTransportStateAugmentationBuilder()
            .setLocalPort(new PortNumber(localPort)).setRemoteAddress(remoteAddress)
            .setRemotePort(new PortNumber(remotePort)).build();
    }

    public static NeighborErrorHandlingStateAugmentation buildErrorHandlingState(final long erroneousUpdateCount) {
        return new NeighborErrorHandlingStateAugmentationBuilder()
            .setErroneousUpdateMessages(erroneousUpdateCount).build();
    }

    public static List<AfiSafi> buildAfisSafisState(
        @Nonnull final Set<Class<? extends AfiSafiType>> afiSafisReceived,
        @Nonnull final Set<Class<? extends AfiSafiType>> afiSafisAdvertized,
        @Nonnull final Set<Class<? extends AfiSafiType>> afiSafisGracefulAdvertized,
        @Nonnull final Set<Class<? extends AfiSafiType>> afiSafisGracefulReceived,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesInstalled,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesReceived,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesSent) {
        final Set<Class<? extends AfiSafiType>> afiSafiJoin = new HashSet<>(afiSafisAdvertized);
        afiSafiJoin.addAll(afiSafisAdvertized);
        return afiSafiJoin.stream().map(
            afiSafi -> buildAfiSafi(afiSafi,
                prefixesInstalled,
                prefixesReceived,
                prefixesSent,
                afiSafisAdvertized.contains(afiSafi),
                afiSafisReceived.contains(afiSafi),
                afiSafisGracefulAdvertized.contains(afiSafi),
                afiSafisGracefulReceived.contains(afiSafi)))
            .collect(Collectors.toList());
    }

    private static AfiSafi buildAfiSafi(final Class<? extends AfiSafiType> afiSafi,
        final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesInstalled,
        final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesReceived,
        final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesSent,
        final boolean afiSafisAdvertized,
        final boolean afiSafiReceived,
        final boolean gracefulRestartAdvertized,
        final boolean gracefulRestartReceived) {

        return new AfiSafiBuilder()
            .setAfiSafiName(afiSafi)
            .setState(buildAfiSafiState(afiSafi, prefixesInstalled, prefixesReceived, prefixesSent,
                afiSafisAdvertized && afiSafiReceived))
            .setGracefulRestart(buildAfiSafiGracefulRestartState(gracefulRestartAdvertized, gracefulRestartReceived))
            .build();
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.
        safi.list.afi.safi.State buildAfiSafiState(@Nonnull final Class<? extends AfiSafiType> afiSafi,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesInstalled,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesReceived,
        @Nonnull final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesSent,
        final boolean afiSafiSupported) {
        final NeighborAfiSafiStateAugmentationBuilder builder = new NeighborAfiSafiStateAugmentationBuilder();
        builder.setActive(afiSafiSupported);
        if (afiSafiSupported) {
            builder.setPrefixes(new PrefixesBuilder().setInstalled(prefixesInstalled.get(afiSafi).getCount())
                .setReceived(prefixesReceived.get(afiSafi).getCount()).setSent(prefixesSent.get(afiSafi).getCount()).build());
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

    public static List<Class<? extends BgpCapability>> buildSupportedCapabilities(final boolean addPath,
        final boolean asn32, final boolean gracefulRestart, final boolean multiProtocol, final boolean routerRefresh) {
        final List<Class<? extends BgpCapability>> supportedCapabilities = new ArrayList<>();
        if (addPath) {
            supportedCapabilities.add(ADDPATHS.class);
        }
        if (asn32) {
            supportedCapabilities.add(ASN32.class);
        }
        if (gracefulRestart) {
            supportedCapabilities.add(GRACEFULRESTART.class);
        }
        if (multiProtocol) {
            supportedCapabilities.add(MPBGP.class);
        }
        if (routerRefresh) {
            supportedCapabilities.add(ROUTEREFRESH.class);
        }
        return supportedCapabilities;
    }
}
