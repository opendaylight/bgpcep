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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.topology.pcep.type.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
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
    private final DataBroker dataProvider;

    public ServerSessionManager(final DataBroker dataProvider, final InstanceIdentifier<Topology> topology,
            final TopologySessionListenerFactory listenerFactory) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.topology = Preconditions.checkNotNull(topology);
        this.listenerFactory = Preconditions.checkNotNull(listenerFactory);

        // FIXME: should migrated to transaction chain
        final ReadWriteTransaction t = dataProvider.newReadWriteTransaction();

        // Make sure the topology does not exist
        final Optional<?> c;
        try {
            c = t.read(LogicalDatastoreType.OPERATIONAL, topology).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to ensure topology presence", e);
        }
        Preconditions.checkArgument(!c.isPresent(), "Topology %s already exists", topology);

        // Now create the base topology
        final TopologyKey k = InstanceIdentifier.keyOf(topology);
        t.put(LogicalDatastoreType.OPERATIONAL, topology, new TopologyBuilder().setKey(k).setTopologyId(k.getTopologyId()).setTopologyTypes(
                new TopologyTypesBuilder().addAugmentation(TopologyTypes1.class,
                        new TopologyTypes1Builder().setTopologyPcep(new TopologyPcepBuilder().build()).build()).build()).setNode(
                                new ArrayList<Node>()).build());

        Futures.addCallback(t.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Topology {} created successfully", topology);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to create topology {}", topology, t);
            }
        });
    }

    public void releaseNodeState(final TopologyNodeState nodeState) {
        LOG.debug("Node {} unbound", nodeState.getNodeId());
        this.nodes.remove(nodeState.getNodeId());
        nodeState.released();
    }

    synchronized TopologyNodeState takeNodeState(final NodeId id, final TopologySessionListener sessionListener) {
        LOG.debug("Node {} bound to listener {}", id, sessionListener);

        TopologyNodeState ret = this.state.get(id);
        if (ret == null) {
            ret = new TopologyNodeState(id, DEFAULT_HOLD_STATE_NANOS);
            this.state.put(id, ret);
        }

        this.nodes.put(id, sessionListener);
        ret.taken();
        return ret;
    }

    @Override
    public PCEPSessionListener getSessionListener() {
        return listenerFactory.createTopologySessionListener(this);
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

    InstanceIdentifier<Topology> getTopology() {
        return topology;
    }

    WriteTransaction beginTransaction() {
        return dataProvider.newWriteOnlyTransaction();
    }

    ReadWriteTransaction rwTransaction() {
        return dataProvider.newReadWriteTransaction();
    }

    <T extends DataObject> ListenableFuture<Optional<T>> readOperationalData(final InstanceIdentifier<T> id) {
        final ReadTransaction t = dataProvider.newReadOnlyTransaction();
        return t.read(LogicalDatastoreType.OPERATIONAL, id);
    }

    @Override
    public void close() throws InterruptedException, ExecutionException {
        final WriteTransaction t = this.dataProvider.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, this.topology);
        t.commit().get();
    }
}
