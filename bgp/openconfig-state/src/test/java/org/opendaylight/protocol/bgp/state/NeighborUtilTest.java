/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.bgp.state.StateProviderImplTest.TABLES_KEY;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class NeighborUtilTest {
    @Mock
    private BGPSessionState sessionState;
    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;
    @Mock
    private BGPAfiSafiState bgpAfiSafiState;
    private State state = State.IDLE;
    private Optional<Class<? extends AfiSafiType>> afiSafi = Optional.empty();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(false).when(this.sessionState).isRouterRefreshCapabilitySupported();
        doReturn(false).when(this.sessionState).isMultiProtocolCapabilitySupported();
        doReturn(false).when(this.sessionState).isGracefulRestartCapabilitySupported();
        doReturn(false).when(this.sessionState).isAsn32CapabilitySupported();
        doReturn(false).when(this.sessionState).isAddPathCapabilitySupported();
        doReturn(this.state).when(this.sessionState).getSessionState();
        doAnswer(invocation -> NeighborUtilTest.this.state).when(this.sessionState).getSessionState();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisAdvertized();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisReceived();
        doAnswer(invocation -> NeighborUtilTest.this.afiSafi).when(this.tableRegistry).getAfiSafiType(eq(TABLES_KEY));
        doReturn(false).when(this.bgpAfiSafiState).isAfiSafiSupported(eq(TABLES_KEY));
        doReturn(false).when(this.bgpAfiSafiState).isGracefulRestartAdvertized(eq(TABLES_KEY));
        doReturn(false).when(this.bgpAfiSafiState).isGracefulRestartReceived(eq(TABLES_KEY));
        doReturn(false).when(this.bgpAfiSafiState).isLlGracefulRestartAdvertised(eq(TABLES_KEY));
        doReturn(false).when(this.bgpAfiSafiState).isLlGracefulRestartReceived(eq(TABLES_KEY));
        doReturn(0).when(this.bgpAfiSafiState).getLlGracefulRestartTimer(eq(TABLES_KEY));


    }

    @Test
    public void testBuildCapabilityState() {
        final NeighborStateAugmentationBuilder expected = new NeighborStateAugmentationBuilder()
                .setSupportedCapabilities(Collections.emptyList())
                .setSessionState(SessionState.IDLE);
        assertEquals(expected.build(), NeighborUtil.buildCapabilityState(this.sessionState));

        this.state = State.OPEN_CONFIRM;
        expected.setSessionState(SessionState.OPENCONFIRM);
        assertEquals(expected.build(), NeighborUtil.buildCapabilityState(this.sessionState));
    }

    @Test
    public void testBuildTimerNullValue() {
        assertNull(NeighborUtil.buildTimer(null));
    }

    @Test
    public void testBuildTimerNormalValue() {
        final BGPTimersState timerState = mock(BGPTimersState.class);
        doReturn(90L).when(timerState).getNegotiatedHoldTime();
        doReturn(5000L).when(timerState).getUpTime();

        final NeighborTimersStateAugmentation timerStateAug = new NeighborTimersStateAugmentationBuilder()
                .setNegotiatedHoldTime(BigDecimal.valueOf(90L)).setUptime(new Timeticks(Uint32.valueOf(500))).build();
        final Timers expectedTimers = new TimersBuilder().setState(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers
                .StateBuilder().addAugmentation(timerStateAug).build())
                .build();
        assertEquals(expectedTimers, NeighborUtil.buildTimer(timerState));
    }

    @Test
    public void testBuildTimerRollOverValue() {
        final BGPTimersState timerState = mock(BGPTimersState.class);
        doReturn(90L).when(timerState).getNegotiatedHoldTime();
        doReturn(42949673015L).when(timerState).getUpTime();

        final NeighborTimersStateAugmentation timerStateAug = new NeighborTimersStateAugmentationBuilder()
                .setNegotiatedHoldTime(BigDecimal.valueOf(90L)).setUptime(new Timeticks(Uint32.valueOf(5))).build();
        final Timers expectedTimers = new TimersBuilder().setState(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers
                .StateBuilder().addAugmentation(timerStateAug).build()).build();
        assertEquals(expectedTimers, NeighborUtil.buildTimer(timerState));
    }

    @Test
    public void testBuildTransport() {
        assertNull(NeighborUtil.buildTransport(null));
    }

    @Test
    public void testBuildNeighborState() {
        assertNull(NeighborUtil.buildNeighborState(null, null));
    }

    @Test
    public void buildAfisSafisState() {
        assertEquals(Collections.emptyList(),
                NeighborUtil.buildAfisSafisState(this.bgpAfiSafiState, this.tableRegistry));

        final GracefulRestart graceful = new GracefulRestartBuilder()
                .setState(new StateBuilder().addAugmentation(
                        new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                                .setAdvertised(false)
                                .setReceived(false)
                                .setLlReceived(false)
                                .setLlAdvertised(false)
                                .setLlStaleTimer(Uint32.ZERO)
                                .build()).build()).build();

        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi
                .list.afi.safi.State afiSafiState = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder()
                .addAugmentation(new NeighborAfiSafiStateAugmentationBuilder().setActive(false).build()).build();

        this.afiSafi = Optional.of(IPV4UNICAST.class);
        final AfiSafi expected = new AfiSafiBuilder().setAfiSafiName(this.afiSafi.get())
                .setState(afiSafiState)
                .setGracefulRestart(graceful).build();
        assertEquals(Collections.singletonList(expected),
                NeighborUtil.buildAfisSafisState(this.bgpAfiSafiState, this.tableRegistry));
    }
}