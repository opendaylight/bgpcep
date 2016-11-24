/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandling;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandlingBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ASN32;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.GRACEFULRESTART;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.MPBGP;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ROUTEREFRESH;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentationBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractStateTest extends AbstractDataBrokerTest {
    final AsNumber as = new AsNumber(72L);
    final static int HOLD_TIMER = 10;
    final static PortNumber LOCAL_PORT = new PortNumber(1790);
    final static PortNumber REMOTE_PORT = new PortNumber(179);
    final static int RESTART_TIME = 15;
    final static boolean PEER_RESTARTING = true;
    final static boolean LOCAL_RESTARTING = true;
    final BgpId bgpId = new BgpId("127.0.0.1");
    final IpAddress neighborAddress = new IpAddress(new Ipv4Address("127.0.0.2"));
    final IpAddress remote = new IpAddress(new Ipv4Address("127.0.0.4"));
    final Set<Class<? extends AfiSafiType>> afiSafisAdvertized = Collections.singleton(IPV4UNICAST.class);
    final Set<Class<? extends AfiSafiType>> afiSafisGracefulAdvertized = Collections.singleton(IPV4UNICAST.class);
    final String ribId = "identifier-test";
    final InstanceIdentifier<Bgp> bgpInstanceIdentifier = InstanceIdentifier.create(NetworkInstances.class)
        .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp")).child(Protocols.class)
        .child(Protocol.class, new ProtocolKey(BGP.class, this.ribId)).augmentation(Protocol1.class).child(Bgp.class);

    Neighbor buildNeighborExpected(final IpAddress neighborAddress, final boolean afiSafiActive,
        final boolean advertizeGraceful, final boolean receivedGraceful, final long timerTicks) {
        long errorMessages = 0L;
        final NeighborGracefulRestartStateAugmentationBuilder gracefulAugmentation
            = new NeighborGracefulRestartStateAugmentationBuilder();
        gracefulAugmentation.setPeerRestarting(false);
        gracefulAugmentation.setLocalRestarting(false);
        gracefulAugmentation.setPeerRestartTime(0);
        BigInteger messages = BigInteger.ZERO;
        final NeighborStateAugmentationBuilder neighborStateAugmentation = new NeighborStateAugmentationBuilder()
            .setSessionState(BgpNeighborState.SessionState.ACTIVE)
            .setSupportedCapabilities(Collections.emptyList());
        BigDecimal holdTime = BigDecimal.ZERO;
        PortNumber localPort = new PortNumber(0);
        PortNumber remotePort = new PortNumber(0);
        IpAddress remoteAddress = null;
        final NeighborAfiSafiStateAugmentationBuilder neighborAfiSafiStateAugmentation =
            new NeighborAfiSafiStateAugmentationBuilder().setActive(false);
        if (afiSafiActive) {
            errorMessages = 1L;
            gracefulAugmentation.setLocalRestarting(LOCAL_RESTARTING)
                .setMode(BgpAfiSafiGracefulRestartState.Mode.BILATERAL)
                .setPeerRestarting(PEER_RESTARTING).setPeerRestartTime(RESTART_TIME);
            messages = BigInteger.ONE;
            neighborStateAugmentation.setSessionState(BgpNeighborState.SessionState.ESTABLISHED)
                .setSupportedCapabilities(Arrays.asList(ASN32.class, ROUTEREFRESH.class, MPBGP.class, ADDPATHS.class,
                    GRACEFULRESTART.class));
            holdTime = BigDecimal.TEN;
            localPort = LOCAL_PORT;
            remotePort = REMOTE_PORT;
            remoteAddress = this.remote;
            neighborAfiSafiStateAugmentation.setActive(true)
                .setPrefixes(new PrefixesBuilder().setSent(1L).setReceived(2L).setInstalled(1L).build());
        }
        final AfiSafi afiSafi = new AfiSafiBuilder()
            .setAfiSafiName(IPV4UNICAST.class)
            .setGracefulRestart(new GracefulRestartBuilder().setState(new StateBuilder().setEnabled(false)
                .addAugmentation(NeighborAfiSafiGracefulRestartStateAugmentation.class,
                    new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                        .setAdvertised(advertizeGraceful).setReceived(receivedGraceful).build())
                .build()).build())
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                .common.afi.safi.list.afi.safi.StateBuilder().setEnabled(false)
                .addAugmentation(NeighborAfiSafiStateAugmentation.class, neighborAfiSafiStateAugmentation.build())
                .build())
            .build();
        final ErrorHandling errorHandling = new ErrorHandlingBuilder().setState(
            new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.error.handling.
                StateBuilder().setTreatAsWithdraw(false)
                .addAugmentation(NeighborErrorHandlingStateAugmentation.class,
                new NeighborErrorHandlingStateAugmentationBuilder().setErroneousUpdateMessages(errorMessages).build()).build())
            .build();
        final GracefulRestart gracefulRestart = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
            .rev151009.bgp.graceful.restart.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http
            .openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
            .addAugmentation(NeighborGracefulRestartStateAugmentation.class, gracefulAugmentation.build()).build()).build();
        final State state = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
            .StateBuilder()
            .setSendCommunity(CommunityType.NONE)
            .setRouteFlapDamping(false)
            .addAugmentation(NeighborStateAugmentation.class, neighborStateAugmentation.build())
            .addAugmentation(BgpNeighborStateAugmentation.class, new BgpNeighborStateAugmentationBuilder()
                .setMessages(new MessagesBuilder().setReceived(new ReceivedBuilder()
                    .setNOTIFICATION(messages).setUPDATE(messages).build())
                    .setSent(new SentBuilder().setNOTIFICATION(messages).setUPDATE(messages).build())
                    .build()).build())
            .build();
        final Timers timers = new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.
            bgp.rev151009.bgp.neighbor.group.timers.StateBuilder()
            .setConnectRetry(BigDecimal.valueOf(30))
            .setHoldTime(BigDecimal.valueOf(90))
            .setKeepaliveInterval(BigDecimal.valueOf(30))
            .setMinimumAdvertisementInterval(BigDecimal.valueOf(30))
            .addAugmentation(NeighborTimersStateAugmentation.class, new NeighborTimersStateAugmentationBuilder()
                .setNegotiatedHoldTime(holdTime).setUptime(new Timeticks(timerTicks)).build())
            .build()).build();
        final Transport transport = new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig
            .net.yang.bgp.rev151009.bgp.neighbor.group.transport.StateBuilder()
            .setMtuDiscovery(false)
            .setPassiveMode(false)
            .addAugmentation(NeighborTransportStateAugmentation.class,
                new NeighborTransportStateAugmentationBuilder().setLocalPort(localPort)
                    .setRemotePort(remotePort)
                    .setRemoteAddress(remoteAddress).build())
            .build()).build();
        return new NeighborBuilder()
            .setNeighborAddress(neighborAddress)
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(Collections.singletonList(afiSafi)).build())
            .setErrorHandling(errorHandling)
            .setGracefulRestart(gracefulRestart)
            .setState(state)
            .setTimers(timers)
            .setTransport(transport)
            .build();
    }

    Global buildGlobalExpected(final long PrefixesAndPaths) {
        return new GlobalBuilder()
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.
                StateBuilder().setRouterId(new Ipv4Address(this.bgpId.getValue())).setTotalPrefixes(PrefixesAndPaths)
                .setTotalPaths(PrefixesAndPaths).setAs(this.as).build())
            .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.
                AfiSafisBuilder().setAfiSafi(Collections.singletonList(new AfiSafiBuilder()
                .setAfiSafiName(IPV4UNICAST.class).setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                    .bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder().setEnabled(false)
                    .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
                        .setTotalPaths(PrefixesAndPaths).setTotalPrefixes(PrefixesAndPaths).build()).build()).build()))
                .build()).build();
    }

    PeerGroup buildGroupExpected(final long totalPaths,
        final long totalPrefixes) {
        return new PeerGroupBuilder().setPeerGroupName("test-group").setState(new org.opendaylight.yang.gen.v1.http
            .openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder()
            .setSendCommunity(CommunityType.NONE)
            .setRouteFlapDamping(false)
            .addAugmentation(PeerGroupStateAugmentation.class,
                new PeerGroupStateAugmentationBuilder().setTotalPaths(totalPaths).setTotalPrefixes(totalPrefixes)
                    .build()).build())
            .build();
    }
}
