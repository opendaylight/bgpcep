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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.state.StateProviderImplTest.TABLES_KEY;

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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.NeighborStateAugmentationBuilder;

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
    public void testBuildTimer() {
        assertNull(NeighborUtil.buildTimer(null));
    }

    @Test
    public void testBuildTransport() {
        assertNull(NeighborUtil.buildTransport(null));
    }

    @Test
    public void testBuildNeighborStatet() {
        assertNull(NeighborUtil.buildNeighborState(null, null));
    }

    @Test
    public void buildAfisSafisState() {
        assertEquals(Collections.emptyList(),
                NeighborUtil.buildAfisSafisState(this.bgpAfiSafiState, this.tableRegistry));

        final GracefulRestart graceful = new GracefulRestartBuilder()
                .setState(new StateBuilder().addAugmentation(NeighborAfiSafiGracefulRestartStateAugmentation.class,
                        new NeighborAfiSafiGracefulRestartStateAugmentationBuilder().setAdvertised(false)
                                .setReceived(false).build()).build()).build();

        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi
                .list.afi.safi.State afiSafiState = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder()
                .addAugmentation(NeighborAfiSafiStateAugmentation.class, new NeighborAfiSafiStateAugmentationBuilder()
                        .setActive(false).build()).build();

        this.afiSafi = Optional.of(IPV4UNICAST.class);
        final AfiSafi expected = new AfiSafiBuilder().setAfiSafiName(this.afiSafi.get())
                .setState(afiSafiState)
                .setGracefulRestart(graceful).build();
        assertEquals(Collections.singletonList(expected),
                NeighborUtil.buildAfisSafisState(this.bgpAfiSafiState, this.tableRegistry));
    }
}