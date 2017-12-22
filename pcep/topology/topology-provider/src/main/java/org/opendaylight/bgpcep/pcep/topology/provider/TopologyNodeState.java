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
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
final class TopologyNodeState implements AutoCloseable, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyNodeState.class);
    private final Map<String, Metadata> metadata = new HashMap<>();
    private final KeyedInstanceIdentifier<Node, NodeKey> nodeId;
    private final BindingTransactionChain chain;
    private final long holdStateNanos;
    private long lastReleased = 0;
    //cache initial node state, if any node was persisted
    private Node initialNodeState = null;

    TopologyNodeState(final DataBroker broker, final InstanceIdentifier<Topology> topology, final NodeId id,
            final long holdStateNanos) {
        Preconditions.checkArgument(holdStateNanos >= 0);
        this.nodeId = topology.child(Node.class, new NodeKey(id));
        this.holdStateNanos = holdStateNanos;
        this.chain = broker.createTransactionChain(this);
    }

    @Nonnull
    KeyedInstanceIdentifier<Node, NodeKey> getNodeId() {
        return this.nodeId;
    }

    synchronized Metadata getLspMetadata(final String name) {
        return this.metadata.get(name);
    }

    synchronized void setLspMetadata(final String name, final Metadata value) {
        if (value == null) {
            this.metadata.remove(name);
        } else {
            this.metadata.put(name, value);
        }
    }

    synchronized void cleanupExcept(final Collection<String> values) {
        this.metadata.keySet().removeIf(s -> !values.contains(s));
    }

    synchronized void released(final boolean persist) {
        // The session went down. Undo all the Topology changes we have done.
        // We might want to persist topology node for later re-use.
        if (!persist) {
            final WriteTransaction trans = beginTransaction();
            trans.delete(LogicalDatastoreType.OPERATIONAL, this.nodeId);
            Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.trace("Internal state for node {} cleaned up successfully", TopologyNodeState.this.nodeId);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Failed to cleanup internal state for session {}",
                            TopologyNodeState.this.nodeId, throwable);
                }
            }, MoreExecutors.directExecutor());
        }

        this.lastReleased = System.nanoTime();
    }

    synchronized void taken(final boolean retrieveNode) {
        final long now = System.nanoTime();

        if (now - this.lastReleased > this.holdStateNanos) {
            this.metadata.clear();
        }

        //try to get the topology's node
        if (retrieveNode) {
            Futures.addCallback(readOperationalData(this.nodeId), new FutureCallback<Optional<Node>>() {
                @Override
                public void onSuccess(@Nonnull final Optional<Node> result) {
                    if (!result.isPresent()) {
                        putTopologyNode();
                    } else {
                        //cache retrieved node
                        TopologyNodeState.this.initialNodeState = result.get();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Failed to get topology node {}", TopologyNodeState.this.nodeId, throwable);
                }
            }, MoreExecutors.directExecutor());
        } else {
            putTopologyNode();
        }
    }

    synchronized Node getInitialNodeState() {
        return this.initialNodeState;
    }

    WriteTransaction beginTransaction() {
        return this.chain.newWriteOnlyTransaction();
    }

    ReadWriteTransaction rwTransaction() {
        return this.chain.newReadWriteTransaction();
    }

    <T extends DataObject> ListenableFuture<Optional<T>> readOperationalData(final InstanceIdentifier<T> id) {
        try (ReadOnlyTransaction t = this.chain.newReadOnlyTransaction()) {
            return t.read(LogicalDatastoreType.OPERATIONAL, id);
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        // FIXME: flip internal state, so that the next attempt to update fails, triggering node reconnect
        LOG.error("Unexpected transaction failure in node {} transaction {}",
                this.nodeId, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Node {} shutdown successfully", this.nodeId);
    }

    @Override
    public void close() {
        this.chain.close();
    }

    private void putTopologyNode() {
        final Node node = new NodeBuilder().setKey(this.nodeId.getKey())
                .setNodeId(this.nodeId.getKey().getNodeId()).build();
        final WriteTransaction t = beginTransaction();
        t.put(LogicalDatastoreType.OPERATIONAL, this.nodeId, node);
        t.submit();
    }

}