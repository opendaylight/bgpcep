/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is thread-safe
final class TopologyNodeState implements FutureCallback<Empty> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyNodeState.class);

    private final Map<String, Metadata> metadata = new HashMap<>();
    private final @NonNull WithKey<Node, NodeKey> nodeId;
    private final TransactionChain chain;
    private final long holdStateNanos;
    private long lastReleased = 0;
    //cache initial node state, if any node was persisted
    @GuardedBy("this")
    private Node initialNodeState = null;

    TopologyNodeState(final DataBroker broker, final WithKey<Topology, TopologyKey> topology, final NodeId id,
            final long holdStateNanos) {
        checkArgument(holdStateNanos >= 0);
        nodeId = topology.toBuilder().child(Node.class, new NodeKey(id)).build();
        this.holdStateNanos = holdStateNanos;
        chain = broker.createMergingTransactionChain();
        chain.addCallback(this);
    }

    @NonNull WithKey<Node, NodeKey> getNodeId() {
        return nodeId;
    }

    synchronized Metadata getLspMetadata(final String name) {
        return metadata.get(name);
    }

    synchronized void setLspMetadata(final String name, final Metadata value) {
        if (value == null) {
            metadata.remove(name);
        } else {
            metadata.put(name, value);
        }
    }

    synchronized void cleanupExcept(final Collection<String> values) {
        metadata.keySet().removeIf(s -> !values.contains(s));
    }

    synchronized void released(final boolean persist) {
        // The session went down. Undo all the Topology changes we have done.
        // We might want to persist topology node for later re-use.
        if (!persist) {
            final WriteTransaction trans = chain.newWriteOnlyTransaction();
            trans.delete(LogicalDatastoreType.OPERATIONAL, nodeId);
            trans.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.trace("Internal state for node {} cleaned up successfully", nodeId);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Failed to cleanup internal state for session {}", nodeId, throwable);
                }
            }, MoreExecutors.directExecutor());
        }

        close();
        lastReleased = System.nanoTime();
    }

    synchronized void taken(final boolean retrieveNode) {
        final long now = System.nanoTime();

        if (now - lastReleased > holdStateNanos) {
            metadata.clear();
        }

        //try to get the topology's node
        if (retrieveNode) {
            try {
                // FIXME: we really should not be performing synchronous operations
                final var prevNode = readOperationalData(nodeId).get();
                if (prevNode.isEmpty()) {
                    putTopologyNode();
                } else {
                    //cache retrieved node
                    initialNodeState = prevNode.orElseThrow();
                }
            } catch (final ExecutionException | InterruptedException throwable) {
                LOG.error("Failed to get topology node {}", nodeId, throwable);
            }
        } else {
            putTopologyNode();
        }
    }

    synchronized Node getInitialNodeState() {
        return initialNodeState;
    }

    synchronized TransactionChain getChain() {
        return chain;
    }

    synchronized <T extends DataObject> FluentFuture<Optional<T>> readOperationalData(
            final DataObjectIdentifier<T> id) {
        try (ReadTransaction t = chain.newReadOnlyTransaction()) {
            return t.read(LogicalDatastoreType.OPERATIONAL, id);
        }
    }

    @Override
    public void onFailure(final Throwable cause) {
        // FIXME: flip internal state, so that the next attempt to update fails, triggering node reconnect
        LOG.error("Unexpected transaction failure in node {}", nodeId, cause);
        close();
    }

    @Override
    public void onSuccess(final Empty result) {
        LOG.info("Node {} shutdown successfully", nodeId);
    }

    synchronized void close() {
        chain.close();
    }

    @Holding("this")
    private void putTopologyNode() {
        final Node node = new NodeBuilder().withKey(nodeId.key()).build();
        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        LOG.trace("Put topology Node {}, value {}", nodeId, node);
        // FIXME: why is this a 'merge' and not a 'put'? This seems to be related to BGPCEP-739, but there is little
        //        evidence as to what exactly was being overwritten
        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeId, node);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Topology Node stored {}, value {}", nodeId, node);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Put topology Node failed {}, value {}", nodeId, node, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    @NonNull FluentFuture<? extends @NonNull CommitInfo> storeNode(final DataObjectIdentifier<Node1> topologyAugment,
            final Node1 ta) {
        LOG.trace("Peer data {} set to {}", requireNonNull(topologyAugment), requireNonNull(ta));
        synchronized (this) {
            final var tx = chain.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.OPERATIONAL, topologyAugment, ta);
            // All set, commit the modifications
            return tx.commit();
        }
    }
}
