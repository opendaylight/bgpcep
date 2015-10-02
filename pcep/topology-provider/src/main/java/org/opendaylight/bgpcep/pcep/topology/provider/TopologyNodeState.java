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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;
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

    public TopologyNodeState(final DataBroker broker, final InstanceIdentifier<Topology> topology, final NodeId id, final long holdStateNanos) {
        Preconditions.checkArgument(holdStateNanos >= 0);
        this.nodeId = topology.child(Node.class, new NodeKey(id));
        this.holdStateNanos = holdStateNanos;
        this.chain = broker.createTransactionChain(this);
    }

    public KeyedInstanceIdentifier<Node, NodeKey> getNodeId() {
        return nodeId;
    }

    public synchronized Metadata getLspMetadata(final String name) {
        return metadata.get(name);
    }

    public synchronized void setLspMetadata(final String name, final Metadata value) {
        if (value == null) {
            metadata.remove(name);
        } else {
            metadata.put(name, value);
        }
    }

    public synchronized void removeLspMetadata(final String name) {
        metadata.remove(name);
    }

    public synchronized void cleanupExcept(final Collection<String> values) {
        final Iterator<String> it = metadata.keySet().iterator();
        while (it.hasNext()) {
            if (!values.contains(it.next())) {
                it.remove();
            }
        }
    }

    public synchronized void released(final boolean persist) {
        // The session went down. Undo all the Topology changes we have done.
        // We might want to persist topology node for later re-use.
        if (!persist) {
            final WriteTransaction trans = beginTransaction();
            trans.delete(LogicalDatastoreType.OPERATIONAL, this.nodeId);
            Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.trace("Internal state for node {} cleaned up successfully", nodeId);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to cleanup internal state for session {}", nodeId, t);
                }
            });
        }

        lastReleased = System.nanoTime();
    }

    public synchronized void taken(final boolean retrieveNode) {
        final long now = System.nanoTime();

        if (now - lastReleased > holdStateNanos) {
            metadata.clear();
        }

        //try to get the topology's node
        if (retrieveNode) {
            Futures.addCallback(readOperationalData(nodeId), new FutureCallback<Optional<Node>>() {

                @Override
                public void onSuccess(final Optional<Node> result) {
                    if (!result.isPresent()) {
                        putTopologyNode();
                    } else {
                        //cache retrieved node
                        initialNodeState = result.get();
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to get topology node {}", nodeId, t);
                }
            });
        } else {
            putTopologyNode();
        }
    }

    public synchronized Node getInitialNodeState() {
        return initialNodeState;
    }

    WriteTransaction beginTransaction() {
        return chain.newWriteOnlyTransaction();
    }

    ReadWriteTransaction rwTransaction() {
        return chain.newReadWriteTransaction();
    }

    <T extends DataObject> ListenableFuture<Optional<T>> readOperationalData(final InstanceIdentifier<T> id) {
        try (final ReadOnlyTransaction t = chain.newReadOnlyTransaction()) {
            return t.read(LogicalDatastoreType.OPERATIONAL, id);
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        // FIXME: flip internal state, so that the next attempt to update fails, triggering node reconnect
        LOG.error("Unexpected transaction failure in node {} transaction {}", nodeId, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Node {} shutdown successfully", nodeId);
    }

    @Override
    public void close() {
        chain.close();
    }

    private void putTopologyNode() {
        final Node node = new NodeBuilder().setKey(nodeId.getKey()).setNodeId(nodeId.getKey().getNodeId()).build();
        final WriteTransaction t = beginTransaction();
        t.put(LogicalDatastoreType.OPERATIONAL, nodeId, node);
        t.submit();
    }

}