/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.stats.rev181109.PcepTopologyNodeStatsAug;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

public class Bgpcep1042Test extends AbstractPCEPSessionTest {
    /**
     * Test of pcep stats update synchronization.
     *
     * <p>Test that {@link SessionStateUpdater#updateStatistics()} is synchronized properly. It is using transaction
     * chain of {@link TopologyNodeState}, so verify there is no race with node reads.
     */
    @Test
    public void testRace() throws Exception {
        final var listener = getSessionListener();
        final var session = getPCEPSession(getLocalPref(), getRemotePref());
        final var nodeState = manager.takeNodeState(session.getRemoteAddress(), listener, false);
        final var updater = new SessionStateUpdater(listener, session, nodeState);
        final var topologyNodeIId = TOPO_IID.toBuilder()
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(PcepTopologyNodeStatsAug.class)
            .build();

        // invoke multiple asynchronous updates and reads
        // if synchronization is incorrect race will occur between update and read
        for (int i = 0; i < 100; i++) {
            CompletableFuture.runAsync(() -> updater.updateStatistics());
            CompletableFuture.runAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        }
        // invoke the update once more in way we can wait for the result
        final var update = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        update.get(2, TimeUnit.SECONDS).get(2, TimeUnit.SECONDS);
        // just to make sure assert a successful asynchronous read
        final var node = CompletableFuture.supplyAsync(() -> nodeState.readOperationalData(topologyNodeIId));
        assertNotNull(node.get().get().orElseThrow().getPcepSessionState());
    }
}
