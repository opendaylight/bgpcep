/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.topology.pcep.type.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
final class ServerSessionManager implements SessionListenerFactory<PCEPSessionListener>, AutoCloseable, TopologySessionRPCs {
    private static final Logger LOG = LoggerFactory.getLogger(ServerSessionManager.class);
    private static final long DEFAULT_HOLD_STATE_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final Map<NodeId, TopologySessionListener> nodes = new HashMap<>();
    private final Map<NodeId, TopologyNodeState> state = new HashMap<>();
    private final TopologySessionListenerFactory listenerFactory;
    private final InstanceIdentifier<Topology> topology;
    private final DataBroker broker;

    public ServerSessionManager(final DataBroker broker, final InstanceIdentifier<Topology> topology,
            final TopologySessionListenerFactory listenerFactory) throws ReadFailedException, TransactionCommitFailedException {
        this.broker = Preconditions.checkNotNull(broker);
        this.topology = Preconditions.checkNotNull(topology);
        this.listenerFactory = Preconditions.checkNotNull(listenerFactory);


        // Make sure the topology does not exist
        final ReadWriteTransaction tx = broker.newReadWriteTransaction();
        final Optional<?> c = tx.read(LogicalDatastoreType.OPERATIONAL, topology).checkedGet();
        Preconditions.checkArgument(!c.isPresent(), "Topology %s already exists", topology);

        // create empty network-topology if not exists
        tx.merge(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(NetworkTopology.class).build(), new NetworkTopologyBuilder().build());
        // Now create the base topology
        final TopologyKey k = InstanceIdentifier.keyOf(topology);
        tx.put(LogicalDatastoreType.OPERATIONAL, topology, new TopologyBuilder().setKey(k).setTopologyId(k.getTopologyId()).setTopologyTypes(
                new TopologyTypesBuilder().addAugmentation(TopologyTypes1.class,
                        new TopologyTypes1Builder().setTopologyPcep(new TopologyPcepBuilder().build()).build()).build()).setNode(
                                new ArrayList<Node>()).build());

        tx.submit().checkedGet();
    }

    private static NodeId createNodeId(final InetAddress addr) {
        return new NodeId("pcc://" + addr.getHostAddress());
    }

    synchronized void releaseNodeState(final TopologyNodeState nodeState, final PCEPSession session) {
        LOG.debug("Node {} unbound", nodeState.getNodeId());
        this.nodes.remove(createNodeId(session.getRemoteAddress()));
        nodeState.released();
    }

    synchronized TopologyNodeState takeNodeState(final InetAddress address, final TopologySessionListener sessionListener) {
        final NodeId id = createNodeId(address);

        LOG.debug("Node {} requested by listener {}", id, sessionListener);
        TopologyNodeState ret = this.state.get(id);

        if (ret == null) {
            ret = new TopologyNodeState(this.broker, this.topology, id, DEFAULT_HOLD_STATE_NANOS);
            LOG.debug("Created topology node {} for id {} at {}", ret, id, ret.getNodeId());
            this.state.put(id, ret);
        }
        // FIXME: else check for conflicting session

        ret.taken();
        this.nodes.put(id, sessionListener);
        LOG.debug("Node {} bound to listener {}", id, sessionListener);
        return ret;
    }

    @Override
    public PCEPSessionListener getSessionListener() {
        return this.listenerFactory.createTopologySessionListener(this);
    }

    @Override
    public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = this.nodes.get(input.getNode());
        if (l == null) {
            LOG.debug("Session for node {} not found", input.getNode());
            return OperationResults.UNSENT.future();
        }

        return l.addLsp(input);
    }

    @Override
    public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = this.nodes.get(input.getNode());
        if (l == null) {
            LOG.debug("Session for node {} not found", input.getNode());
            return OperationResults.UNSENT.future();
        }

        return l.removeLsp(input);
    }

    @Override
    public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = this.nodes.get(input.getNode());
        if (l == null) {
            LOG.debug("Session for node {} not found", input.getNode());
            return OperationResults.UNSENT.future();
        }

        return l.updateLsp(input);
    }

    @Override
    public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = this.nodes.get(input.getNode());
        if (l == null) {
            LOG.debug("Session for node {} not found", input.getNode());
            return OperationResults.UNSENT.future();
        }

        return l.ensureLspOperational(input);
    }

    @Override
    public void close() throws TransactionCommitFailedException {
        for (final TopologySessionListener sessionListener : this.nodes.values()) {
            sessionListener.close();
        }
        for (final TopologyNodeState nodeState : this.state.values()) {
            nodeState.close();
        }
        final WriteTransaction t = this.broker.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, this.topology);
        t.submit().checkedGet();
    }
}
