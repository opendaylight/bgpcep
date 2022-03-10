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
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class NeighborUtilTest {
    @Mock
    private BGPSessionState sessionState;
    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;
    @Mock
    private BGPAfiSafiState bgpAfiSafiState;
    private State state = State.IDLE;
    private Class<? extends AfiSafiType> afiSafi = null;

    @Before
    public void setUp() throws Exception {
        doReturn(false).when(sessionState).isRouterRefreshCapabilitySupported();
        doReturn(false).when(sessionState).isMultiProtocolCapabilitySupported();
        doReturn(false).when(sessionState).isGracefulRestartCapabilitySupported();
        doReturn(false).when(sessionState).isAsn32CapabilitySupported();
        doReturn(false).when(sessionState).isAddPathCapabilitySupported();
        doAnswer(invocation -> NeighborUtilTest.this.state).when(sessionState).getSessionState();
        doReturn(Collections.singleton(TABLES_KEY)).when(bgpAfiSafiState).getAfiSafisAdvertized();
        doReturn(Collections.singleton(TABLES_KEY)).when(bgpAfiSafiState).getAfiSafisReceived();
        doAnswer(invocation -> NeighborUtilTest.this.afiSafi).when(tableRegistry).getAfiSafiType(eq(TABLES_KEY));
        doReturn(false).when(bgpAfiSafiState).isAfiSafiSupported(eq(TABLES_KEY));
        doReturn(false).when(bgpAfiSafiState).isGracefulRestartAdvertized(eq(TABLES_KEY));
        doReturn(false).when(bgpAfiSafiState).isGracefulRestartReceived(eq(TABLES_KEY));
        doReturn(false).when(bgpAfiSafiState).isLlGracefulRestartAdvertised(eq(TABLES_KEY));
        doReturn(false).when(bgpAfiSafiState).isLlGracefulRestartReceived(eq(TABLES_KEY));
        doReturn(0).when(bgpAfiSafiState).getLlGracefulRestartTimer(eq(TABLES_KEY));
    }

    @Test
    public void testBuildCapabilityState() {
        final NeighborStateAugmentationBuilder expected = new NeighborStateAugmentationBuilder()
                .setSupportedCapabilities(Set.of())
                .setSessionState(SessionState.IDLE);
        assertEquals(expected.build(), NeighborUtil.buildCapabilityState(sessionState));

        state = State.OPEN_CONFIRM;
        expected.setSessionState(SessionState.OPENCONFIRM);
        assertEquals(expected.build(), NeighborUtil.buildCapabilityState(sessionState));
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

        final Timers expectedTimers = new TimersBuilder()
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers
                .StateBuilder()
                    .addAugmentation(new NeighborTimersStateAugmentationBuilder()
                        .setNegotiatedHoldTime(Decimal64.valueOf("90.00"))
                        .setUptime(new Timeticks(Uint32.valueOf(500)))
                        .build())
                    .build())
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
        assertEquals(Collections.emptyMap(),
                NeighborUtil.buildAfisSafisState(bgpAfiSafiState, tableRegistry));

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

        afiSafi = IPV4UNICAST.class;
        final AfiSafi expected = new AfiSafiBuilder().setAfiSafiName(afiSafi)
                .setState(afiSafiState)
                .setGracefulRestart(graceful).build();
        assertEquals(Collections.singletonMap(expected.key(), expected),
                NeighborUtil.buildAfisSafisState(bgpAfiSafiState, tableRegistry));
    }
}