/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods required to manage the path
 * representation elements in the Data Store.
 *
 * @author Olivier Dugeon
 */

public final class PathManagerListener
        implements DataTreeChangeListener<ManagedNode>, AutoCloseable, @NonNull TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerListener.class);
    private static final InstanceIdentifier<ManagedPath> MANAGED_PATH_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).build();
    private static final InstanceIdentifier<ManagedNode> MANAGED_NODE_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).child(ManagedNode.class).build();

    private ListenerRegistration<PathManagerListener> listenerRegistration;
    private final DataBroker dataBroker;
    private TransactionChain chain = null;
    private final PathManagerProvider pathManager;

    public PathManagerListener(final DataBroker dataBroker, final PathManagerProvider pathManager) {
        this.dataBroker = requireNonNull(dataBroker);
        requireNonNull(pathManager);
        this.pathManager = pathManager;
        initTransactionChain();
        initPathManager();
        this.listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, MANAGED_NODE_IDENTIFIER), this);
        LOG.info("Registered listener {} for Managed Path {}", this, MANAGED_NODE_IDENTIFIER);
    }

    /**
     * Close this Listener.
     */
    @Override
    public void close() {
        if (this.listenerRegistration != null) {
            LOG.debug("Unregistered listener {} for Managed Path", this);
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for Path Manager Server {}", this);
        Preconditions.checkState(this.chain == null, "Transaction chain has to be closed before being initialized");
        this.chain = this.dataBroker.createMergingTransactionChain(this);
    }

    /**
     * Initialize Managed Path tree at Data Store top-level.
     */
    private synchronized void initPathManager() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        LOG.info("Create Path Manager at top level in Configuration DataStore: {}", MANAGED_PATH_IDENTIFIER);
        trans.put(LogicalDatastoreType.CONFIGURATION, MANAGED_PATH_IDENTIFIER, new ManagedPathBuilder().build());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize Path Manager {} (transaction {}) by listener {}",
                    MANAGED_PATH_IDENTIFIER, trans.getIdentifier(), PathManagerListener.this, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Parse Sub Tree modification. This method is called with the Modified Children from
     * the Data Tree Modification root. This method is necessary as the getModificationType() method returns
     * SUBTREE_MODIFIED only when Data Object is already present in the Data Store. Thus, this indication is only
     * relevant for deletion not for insertion where WRITE modification type is return even if it concerns a child.
     *
     * @param children List of Paths for this PCC
     */
    private void parseSubTree(NodeId nodeId,
            final Collection<? extends DataObjectModification<? extends DataObject>> children) {
        for (DataObjectModification<? extends DataObject> child : children) {
            TePath tePath;
            switch (child.getModificationType()) {
                case DELETE:
                    tePath = (TePath )child.getDataBefore();
                    LOG.info("Got Deletion for TE Path {}", tePath.getName());
                    pathManager.deleteManagedTePath(nodeId, tePath.key());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    tePath = (TePath )child.getDataAfter();
                    LOG.info("Got Write Action for TE Path {}", tePath.getName());
                    pathManager.createManagedTePath(nodeId, tePath);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<ManagedNode>> changes) {
        for (DataTreeModification<ManagedNode> change : changes) {
            DataObjectModification<ManagedNode> root = change.getRootNode();
            ManagedNode node;
            switch (root.getModificationType()) {
                case DELETE:
                    node = root.getDataBefore();
                    LOG.info("Delete Managed Node {}", node.getNodeId());
                    pathManager.deleteManageTeNode(node);
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    /* First look if the PCC was not already created */
                    node = root.getDataAfter();
                    LOG.info("Got Write action for Managed Node {}", node.getNodeId());
                    if (!pathManager.checkManagedTeNode(node.getNodeId())) {
                        LOG.info("Create new Managed Node {}", node.getNodeId());
                        pathManager.createManagedTeNode(node);
                    } else {
                        /* PCC exist, process Children */
                        parseSubTree(node.getNodeId(), root.getModifiedChildren());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain transactionChain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Path Manager builder for {} failed in transaction: {} ", MANAGED_NODE_IDENTIFIER,
                transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain transactionChain) {
        LOG.info("Path Manager builder for {} shut down", MANAGED_NODE_IDENTIFIER);
    }
}

