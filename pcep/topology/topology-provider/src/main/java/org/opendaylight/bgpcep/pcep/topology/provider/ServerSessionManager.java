/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TearDownSessionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.network.topology.topology.topology.types.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Non-final for testing
class ServerSessionManager implements PCEPSessionListenerFactory, TopologySessionRPCs {
    @FunctionalInterface
    interface RpcTimeout {
        void run(SrpIdNumber requestId);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerSessionManager.class);
    private static final long DEFAULT_HOLD_STATE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final VarHandle CLOSED;

    static {
        try {
            CLOSED = MethodHandles.lookup().findVarHandle(ServerSessionManager.class, "closed", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @NonNull WithKey<Topology, TopologyKey> topology;
    private final @NonNull PCEPTopologyProviderDependencies dependencies;
    private final @Nullable GraphKey graphKey;

    @GuardedBy("this")
    private final Map<NodeId, TopologySessionListener> nodes = new HashMap<>();
    @GuardedBy("this")
    private final Map<NodeId, TopologyNodeState> state = new HashMap<>();

    private volatile long updateInterval;
    private volatile short rpcTimeout;
    private volatile boolean closed;

    ServerSessionManager(final WithKey<Topology, TopologyKey> topology,
            final PCEPTopologyProviderDependencies dependencies, final GraphKey graphKey,
            final short rpcTimeout, final long updateInterval) {
        this.dependencies = requireNonNull(dependencies);
        this.topology = requireNonNull(topology);
        this.graphKey = graphKey;
        this.rpcTimeout = rpcTimeout;
        this.updateInterval = updateInterval;
    }

    // Initialize the operational view of the topology.
    final ListenableFuture<Boolean> start() {
        LOG.info("Creating PCEP Topology {}", topologyId());
        final var tx = dependencies.getDataBroker().newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, topology, new TopologyBuilder()
            .withKey(topology.key())
            .setTopologyTypes(new TopologyTypesBuilder()
                .addAugmentation(new TopologyTypes1Builder().setTopologyPcep(new TopologyPcepBuilder().build()).build())
                .build())
            .build());

        final var future = SettableFuture.<Boolean>create();
        final var txFuture = tx.commit();
        txFuture.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("PCEP Topology {} created successfully.", topologyId());
                closed = false;
                future.set(Boolean.TRUE);
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.error("Failed to create PCEP Topology {}.", topologyId(), failure);
                closed = true;
                future.set(Boolean.FALSE);
            }
        }, MoreExecutors.directExecutor());

        // Register this new topology to PCE Server
        final PceServerProvider server = dependencies.getPceServerProvider();
        if (server != null && graphKey != null) {
            server.registerPcepTopology(topology, graphKey);
        }
        return future;
    }

    final boolean isClosed() {
        return closed;
    }

    final synchronized FluentFuture<? extends CommitInfo> stop() {
        if (!CLOSED.compareAndSet(this, false, true)) {
            LOG.error("Session Manager has already been closed.");
            return CommitInfo.emptyFluentFuture();
        }

        // Clean up sessions
        for (final TopologySessionListener node : nodes.values()) {
            node.close();
        }
        nodes.clear();

        // Clean up remembered metadata
        for (final TopologyNodeState topologyNodeState : state.values()) {
            topologyNodeState.close();
        }
        state.clear();

        // Un-Register Pcep Topology into PCE Server
        final PceServerProvider server = dependencies.getPceServerProvider();
        if (server != null) {
            server.unRegisterPcepTopology(topology);
        }

        final WriteTransaction tx = dependencies.getDataBroker().newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, topology);
        final var future = tx.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Topology {} removed", topology);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed to remove Topology {}", topology, throwable);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    final synchronized void releaseNodeState(final TopologyNodeState nodeState, final InetAddress peerAddress,
            final boolean persistNode) {
        if (isClosed()) {
            LOG.error("Session Manager has already been closed.");
            return;
        }
        final NodeId nodeId = createNodeId(peerAddress);
        nodes.remove(nodeId);
        state.remove(nodeId);
        if (nodeState != null) {
            LOG.debug("Node {} unbound", nodeState.getNodeId());
            nodeState.released(persistNode);
        }
    }

    final synchronized TopologyNodeState takeNodeState(final InetAddress address,
            final TopologySessionListener sessionListener, final boolean retrieveNode) {
        final NodeId id = createNodeId(address);
        if (isClosed()) {
            LOG.error("Server Session Manager is closed. Unable to create topology node {} with listener {}", id,
                sessionListener);
            return null;
        }

        LOG.debug("Node {} requested by listener {}", id, sessionListener);
        TopologyNodeState ret = state.get(id);

        if (ret == null) {
            ret = new TopologyNodeState(dependencies.getDataBroker(), topology, id, DEFAULT_HOLD_STATE_NANOS);
            LOG.debug("Created topology node {} for id {} at {}", ret, id, ret.getNodeId());
            state.put(id, ret);
        }
        // if another listener requests the same session, close it
        final TopologySessionListener existingSessionListener = nodes.get(id);
        if (existingSessionListener != null && !sessionListener.equals(existingSessionListener)) {
            LOG.error("New session listener {} is in conflict with existing session listener {} on node {},"
                    + " closing the existing one.", existingSessionListener, sessionListener, id);
            existingSessionListener.close();
        }
        ret.taken(retrieveNode);
        nodes.put(id, sessionListener);
        LOG.debug("Node {} bound to listener {}", id, sessionListener);
        return ret;
    }

    // Non-final for testing
    @Override
    public PCEPTopologySessionListener getSessionListener() {
        return new PCEPTopologySessionListener(dependencies.getStateRegistry(), this,
            dependencies.getPceServerProvider());
    }

    private synchronized TopologySessionListener checkSessionPresence(final NodeId nodeId) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = nodes.get(nodeId);
        if (l == null) {
            LOG.debug("Session for node {} not found", nodeId);
            return null;
        }
        return l;
    }

    @Override
    public final synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.addLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public final synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.removeLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public final synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.updateLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public final synchronized ListenableFuture<OperationResult> ensureLspOperational(
            final EnsureLspOperationalInput input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.ensureLspOperational(input) : OperationResults.UNSENT.future();
    }

    @Override
    public final synchronized ListenableFuture<OperationResult> triggerSync(final TriggerSyncArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.triggerSync(input) : OperationResults.UNSENT.future();
    }

    @Override
    public final ListenableFuture<RpcResult<Void>> tearDownSession(final TearDownSessionInput input) {
        final NodeId nodeId = input.getNode();
        final TopologySessionListener listener = checkSessionPresence(nodeId);
        if (listener != null) {
            return listener.tearDownSession(input);
        }

        return RpcResultBuilder.<Void>failed()
            .withError(ErrorType.RPC, "Failed to find session " + nodeId)
            .buildFuture();
    }

    final @Nullable Timeout newRpcTimeout(final RpcTimeout task, final SrpIdNumber requestId) {
        final short localTimeout = rpcTimeout;
        return localTimeout <= 0 ? null
            : dependencies.getTimer().newTimeout(ignored -> task.run(requestId), localTimeout, TimeUnit.SECONDS);
    }

    final long updateInterval() {
        return isClosed() ? 0 : updateInterval;
    }

    final void setRpcTimeout(final short rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
    }

    final void setUpdateInterval(final long updateInterval) {
        this.updateInterval = updateInterval;
    }

    final void tearDownSessions(final List<InetAddress> outdatedNodes) {
        for (var address : outdatedNodes) {
            tearDownSession(new TearDownSessionInputBuilder().setNode(createNodeId(address)).build());
        }
    }

    final PCEPTopologyProviderDependencies getPCEPTopologyProviderDependencies() {
        return dependencies;
    }

    static @NonNull NodeId createNodeId(final InetAddress addr) {
        return new NodeId("pcc://" + addr.getHostAddress());
    }

    private @NonNull String topologyId() {
        return TopologyUtils.friendlyId(topology);
    }
}