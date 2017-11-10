/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.pcep.rpc.rev171110.ReleaseConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.topology.pcep.type.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
final class ServerSessionManager implements PCEPSessionListenerFactory, TopologySessionRPCs, PCEPPeerProposal {
    private static final Logger LOG = LoggerFactory.getLogger(ServerSessionManager.class);
    private static final long DEFAULT_HOLD_STATE_NANOS = TimeUnit.MINUTES.toNanos(5);

    @GuardedBy("this")
    private final Map<NodeId, TopologySessionListener> nodes = new HashMap<>();
    @GuardedBy("this")
    private final Map<NodeId, TopologyNodeState> state = new HashMap<>();
    private final TopologySessionListenerFactory listenerFactory;
    private final InstanceIdentifier<Topology> topology;
    private final DataBroker broker;
    private final PCEPStatefulPeerProposal peerProposal;
    private final short rpcTimeout;

    @VisibleForTesting
    public final AtomicBoolean isClosed = new AtomicBoolean(false);

    public ServerSessionManager(final DataBroker broker, final InstanceIdentifier<Topology> topology,
            final TopologySessionListenerFactory listenerFactory, final short rpcTimeout) {
        this.broker = requireNonNull(broker);
        this.topology = requireNonNull(topology);
        this.listenerFactory = requireNonNull(listenerFactory);
        this.peerProposal = PCEPStatefulPeerProposal.createStatefulPeerProposal(this.broker, this.topology);
        this.rpcTimeout = rpcTimeout;
    }

    /**
     * Create Base Topology
     */
    synchronized ListenableFuture<Void> instantiateServiceInstance() {
        final TopologyKey key = InstanceIdentifier.keyOf(this.topology);
        final TopologyId topologyId = key.getTopologyId();
        final WriteTransaction tx = this.broker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, this.topology, new TopologyBuilder().setKey(key)
                .setTopologyId(topologyId).setTopologyTypes(new TopologyTypesBuilder()
                        .addAugmentation(TopologyTypes1.class, new TopologyTypes1Builder().setTopologyPcep(
                                new TopologyPcepBuilder().build()).build()).build())
                .setNode(new ArrayList<>()).build(), true);
        final ListenableFuture<Void> future = tx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("PCEP Topology {} created successfully.", topologyId.getValue());
                ServerSessionManager.this.isClosed.set(false);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to create PCEP Topology {}.", topologyId.getValue(), t);
                ServerSessionManager.this.isClosed.set(true);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    private static NodeId createNodeId(final InetAddress addr) {
        return new NodeId("pcc://" + addr.getHostAddress());
    }

    synchronized void releaseNodeState(final TopologyNodeState nodeState, final PCEPSession session, final boolean persistNode) {
        if (this.isClosed.get()) {
            LOG.error("Session Manager has already been closed.");
            return;
        }
        this.nodes.remove(createNodeId(session.getRemoteAddress()));
        if (nodeState != null) {
            LOG.debug("Node {} unbound", nodeState.getNodeId());
            nodeState.released(persistNode);
        }
    }

    synchronized TopologyNodeState takeNodeState(final InetAddress address, final TopologySessionListener sessionListener, final boolean retrieveNode) {
        final NodeId id = createNodeId(address);
        if (this.isClosed.get()) {
            LOG.error("Server Session Manager is closed. Unable to create topology node {} with listener {}", id, sessionListener);
            return null;
        }

        LOG.debug("Node {} requested by listener {}", id, sessionListener);
        TopologyNodeState ret = this.state.get(id);

        if (ret == null) {
            ret = new TopologyNodeState(this.broker, this.topology, id, DEFAULT_HOLD_STATE_NANOS);
            LOG.debug("Created topology node {} for id {} at {}", ret, id, ret.getNodeId());
            this.state.put(id, ret);
        }
        // if another listener requests the same session, close it
        final TopologySessionListener existingSessionListener = this.nodes.get(id);
        if (existingSessionListener != null && !sessionListener.equals(existingSessionListener)) {
            LOG.error("New session listener {} is in conflict with existing session listener {} on node {}, closing the existing one.", existingSessionListener, sessionListener, id);
            existingSessionListener.close();
        }
        ret.taken(retrieveNode);
        this.nodes.put(id, sessionListener);
        LOG.debug("Node {} bound to listener {}", id, sessionListener);
        return ret;
    }

    @Override
    public PCEPSessionListener getSessionListener() {
        return this.listenerFactory.createTopologySessionListener(this);
    }

    protected final synchronized TopologySessionListener checkSessionPresence(final NodeId nodeId) {
        // Get the listener corresponding to the node
        final TopologySessionListener l = this.nodes.get(nodeId);
        if (l == null) {
            LOG.debug("Session for node {} not found", nodeId);
            return null;
        }
        return l;
    }

    @Override
    public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.addLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.removeLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.updateLsp(input) : OperationResults.UNSENT.future();
    }

    @Override
    public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.ensureLspOperational(input) : OperationResults.UNSENT.future();
    }

    @Override
    public synchronized ListenableFuture<OperationResult> triggerSync(final TriggerSyncArgs input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.triggerSync(input) : OperationResults.UNSENT.future();
    }

    @Override
    public ListenableFuture<Void> releaseSession(final ReleaseConnectionInput input) {
        final TopologySessionListener l = checkSessionPresence(input.getNode());
        return l != null ? l.releaseSession(input) : Futures.immediateFailedFuture(new Throwable("Node not present"));
    }

    synchronized ListenableFuture<Void> closeServiceInstance() {
        if (this.isClosed.getAndSet(true)) {
            LOG.error("Session Manager has already been closed.");
            Futures.immediateFuture(null);
        }

        for (final TopologySessionListener sessionListener : this.nodes.values()) {
            sessionListener.close();
        }
        this.nodes.clear();
        for (final TopologyNodeState nodeState : this.state.values()) {
            nodeState.close();
        }
        this.state.clear();
        final WriteTransaction t = this.broker.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, this.topology);
        final ListenableFuture<Void> future = t.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Topology {} removed", ServerSessionManager.this.topology);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.warn("Failed to remove Topology {}", ServerSessionManager.this.topology, t);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public void setPeerSpecificProposal(final InetSocketAddress address, final TlvsBuilder openBuilder) {
        requireNonNull(address);
        this.peerProposal.setPeerProposal(createNodeId(address.getAddress()), openBuilder);
    }

    short getRpcTimeout() {
        return this.rpcTimeout;
    }
}