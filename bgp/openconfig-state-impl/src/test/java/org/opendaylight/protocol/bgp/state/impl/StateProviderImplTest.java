/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.bgp.rib.spi.ReadDataBrokerUtil;
import org.opendaylight.protocol.bgp.state.spi.BGPStateProvider;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;

public class StateProviderImplTest extends AbstractStateTest {
    @Test
    public void testStateProvider() throws ReadFailedException {
        final BGPGlobalStateImpl bgpGlobalState = new BGPGlobalStateImpl(this.afiSafisAdvertized, this.ribId,
            this.bgpId, this.as, this.bgpInstanceIdentifier);
        final BGPStateProvider stateProvider = new StateProviderImpl(getDataBroker(), 1);
        final AbstractRegistration globalRegistry = stateProvider.registerBGPState(bgpGlobalState);

        final Global globalExpected = buildGlobalExpected(0);
        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected, global);
            return bgpRib;
        });

        bgpGlobalState.getPrefixesCounter(IPV4UNICAST.class).incrementCount();
        bgpGlobalState.getPathCounter(IPV4UNICAST.class).incrementCount();

        final Global globalExpected2 = buildGlobalExpected(1);
        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected2, global);
            return bgpRib;
        });

        bgpGlobalState.getPathCounter(IPV4UNICAST.class).decrementCount();
        bgpGlobalState.getPrefixesCounter(IPV4UNICAST.class).decrementCount();

        final Global globalExpected3 = buildGlobalExpected(0);
        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected3, global);
            Assert.assertNull(bgpRib.getNeighbors());
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        final BGPNeighborState neighborState = new BGPNeighborStateImpl(this.neighborAddress, this.afiSafisAdvertized,
            Collections.emptySet());

        final AbstractRegistration registration = bgpGlobalState.registerNeighbor(neighborState, null);
        final Neighbor neighborExpected = buildNeighborExpected(this.neighborAddress, false, false, false, 0L);

        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            Assert.assertEquals(neighborExpected, neighbors.getNeighbor().get(0));
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        registration.close();

        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            Assert.assertNull(bgpRib.getNeighbors());
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        final AbstractRegistration registration2 = bgpGlobalState.registerNeighbor(neighborState, "test-group");

        final PeerGroup peerGroupExpected = buildGroupExpected(0L, 0L);
        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            final Neighbor neighbor = neighbors.getNeighbor().get(0);
            final PeerGroup peerGroup = bgpRib.getPeerGroups().getPeerGroup().get(0);
            Assert.assertEquals(neighborExpected, neighbor);
            Assert.assertEquals(peerGroupExpected, peerGroup);
            return bgpRib;
        });

        neighborState.setActiveAfiSafi(this.afiSafisAdvertized, this.afiSafisGracefulAdvertized);
        neighborState.setAfiSafiGracefulRestartState(RESTART_TIME, PEER_RESTARTING, LOCAL_RESTARTING,
            BgpAfiSafiGracefulRestartState.Mode.BILATERAL);
        neighborState.setCapabilities(HOLD_TIMER, LOCAL_PORT, this.remote, REMOTE_PORT, true, true, true,
            true, true);
        neighborState.setState(BgpNeighborState.SessionState.ESTABLISHED);
        neighborState.getErroneousUpdateReceivedCounter().incrementCount();
        neighborState.getMessagesSentCounter().incrementNotification();
        neighborState.getMessagesSentCounter().incrementUpdate();
        neighborState.getMessagesReceivedCounter().incrementNotification();
        neighborState.getMessagesReceivedCounter().incrementUpdate();
        neighborState.getPrefixesInstalledCounter(IPV4UNICAST.class).incrementCount();
        neighborState.getPrefixesReceivedCounter(IPV4UNICAST.class).incrementCount();
        neighborState.getPrefixesReceivedCounter(IPV4UNICAST.class).incrementCount();
        neighborState.getPrefixesSentCounter(IPV4UNICAST.class).incrementCount();

        final PeerGroup peerGroupExpected2 = buildGroupExpected(1L, 1L);
        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            final Neighbor neighbor = neighbors.getNeighbor().get(0);
            final Long timerTicks = neighbor.getTimers().getState()
                .getAugmentation(NeighborTimersStateAugmentation.class).getUptime().getValue();
            final Neighbor expected = buildNeighborExpected(this.neighborAddress, true, false, true, timerTicks);
            final PeerGroup peerGroup = bgpRib.getPeerGroups().getPeerGroup().get(0);
            Assert.assertEquals(expected, neighbor);
            Assert.assertEquals(peerGroupExpected2, peerGroup);

            return bgpRib;
        });

        registration2.close();

        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            Assert.assertNull(bgpRib.getNeighbors());
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        final BGPNeighborState neighborStateGraceful = new BGPNeighborStateImpl(this.neighborAddress, this
            .afiSafisAdvertized, this.afiSafisGracefulAdvertized);

        final AbstractRegistration registration3 = bgpGlobalState.registerNeighbor(neighborStateGraceful, null);
        final Neighbor neighborExpected3 = buildNeighborExpected(this.neighborAddress, false, true, false, 0L);

        ReadDataBrokerUtil.readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            Assert.assertEquals(neighborExpected3, neighbors.getNeighbor().get(0));
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        registration3.close();
        globalRegistry.close();
    }
}