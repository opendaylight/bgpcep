/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.netty.util.HashedWheelTimer;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.grouping.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

public class Bgpcep1037Test extends AbstractPCEPSessionTest {
    @Mock
    private SessionStateUpdater sessionStateUpdater;

    /**
     * Test of invoking pcep stats update.
     *
     * <p>Verify that {@link TopologyStatsProvider} correctly schedule {@link SessionStateUpdater#updateStatistics()}
     * after {@link SessionStateUpdater} is registered.
     */
    @Test
    public void testUpdateStatisticsIsCalled() {
        final var topologyStatsProvider = new TopologyStatsProvider(new HashedWheelTimer());
        doReturn(TimeUnit.MILLISECONDS.toNanos(100)).when(sessionStateUpdater).updateInterval();
        topologyStatsProvider.bind(sessionStateUpdater);

        verify(sessionStateUpdater, timeout(1000)).updateStatistics();
    }

    /**
     * Test of pcep stats update.
     *
     * <p>Test that {@link PcepSessionState} is updated by {@link SessionStateUpdater}.
     */
    @Test
    public void testPcepSessionStateUpdate() throws Exception {
        final var listener = getSessionListener();
        final var session = getPCEPSession(getLocalPref(), getRemotePref());
        final var updater = new SessionStateUpdater(listener, session,
            manager.takeNodeState(session.getRemoteAddress(), listener, false));
        final var topologyNodeIId = TOPO_IID.toBuilder()
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(PcepTopologyNodeStatsAug.class)
            .build();

        updater.updateStatistics().get(5, TimeUnit.SECONDS);
        final var node = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.OPERATIONAL, topologyNodeIId).get().orElseThrow();
        assertNotNull(node.getPcepSessionState());
    }
}
