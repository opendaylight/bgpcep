/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.opendaylight.bgpcep.pcep.server.PathManager;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.ManagedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.ManagedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.NodeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.ManagedNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.ManagedNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.te.path.ActualPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.te.path.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.te.path.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods
 * required to manage the paths in the datastore.
 *
 * @author Olivier Dugeon
 */

public final class PathManagerProvider implements PathManager, TransactionChainListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerProvider.class);
    private static final InstanceIdentifier<ManagedPath> MANAGED_PATH_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).build();

    private final Table<NodeId, TePathKey, TePath> tePaths = HashBasedTable.create();
    private final Map<NodeId, ManagedNode> operNodes = new HashMap<>();
    private final DataBroker dataBroker;
    private final PceServerProvider pceServerProvider;
    private final NetworkTopologyPcepService ntps;
    private TransactionChain chain = null;

    public PathManagerProvider(final DataBroker dataBroker, final NetworkTopologyPcepService ntps,
            final PceServerProvider pceServerProvider) {
        this.dataBroker = requireNonNull(dataBroker);
        this.pceServerProvider = requireNonNull(pceServerProvider);
        this.ntps = requireNonNull(ntps);
        initTransactionChain();
        initOperationalPathManager();
        LOG.info("Path Manager Server started");
    }

    /**
     * Remove the Path Manager Server and destroy the transaction chain.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        destroyOperationalPathManager();
        destroyTransactionChain();
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for Path Manager Server {}", this);
        checkState(this.chain == null, "Transaction chain has to be closed before being initialized");
        this.chain = this.dataBroker.createMergingTransactionChain(this);
    }

    /**
     * Initialize PathManager tree at Data Store top-level.
     */
    private synchronized void initOperationalPathManager() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        LOG.info("Create Path Manager at top level in Operational DataStore: {}", MANAGED_PATH_IDENTIFIER);
        trans.put(LogicalDatastoreType.OPERATIONAL, MANAGED_PATH_IDENTIFIER, new ManagedPathBuilder().build());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize Path Manager {} (transaction {}) by listener {}",
                        MANAGED_PATH_IDENTIFIER, trans.getIdentifier(), PathManagerProvider.this, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided.
     */
    private synchronized FluentFuture<? extends CommitInfo> destroyOperationalPathManager() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, MANAGED_PATH_IDENTIFIER);
        trans.delete(LogicalDatastoreType.CONFIGURATION, MANAGED_PATH_IDENTIFIER);
        final FluentFuture<? extends CommitInfo> future = trans.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Operational Path Manager removed {}", MANAGED_PATH_IDENTIFIER);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to reset operational Path Manager {} (transaction {})", MANAGED_PATH_IDENTIFIER,
                    trans.getIdentifier(), throwable);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Destroy the current transaction chain.
     */
    private synchronized void destroyTransactionChain() {
        if (this.chain != null) {
            LOG.debug("Destroy transaction chain for Path Manager {}", this);
            this.chain = null;
        }
    }

    /**
     * Reset the transaction chain only so that the PingPong transaction chain
     * will become usable again. However, there will be data loss if we do not
     * apply the previous failed transaction again
     */
    protected synchronized void resetTransactionChain() {
        LOG.debug("Resetting transaction chain for Path Manager");
        destroyTransactionChain();
        initTransactionChain();
    }

    /**
     *  DataStore Instance Identifier creation for the ManagedNode and TePath components.
     */
    private static InstanceIdentifier<ManagedNode> getManagedNodeInstanceIdentifier(final NodeId id) {
        return MANAGED_PATH_IDENTIFIER.child(ManagedNode.class, new ManagedNodeKey(id));
    }

    private static InstanceIdentifier<TePath> getTePathInstanceIdentifier(final NodeId id, final TePath path) {
        return MANAGED_PATH_IDENTIFIER.child(ManagedNode.class, new ManagedNodeKey(id)).child(TePath.class, path.key());
    }

    /**
     * Add Managed Node or TE Path components to the Data Store.
     *
     * @param <T>   As a generic method, T must be a Managed Node or a TE Path.
     * @param id    Instance Identifier of the Data Object
     * @param data  Data Object (Managed Node or TE Path)
     * @param info  Information to be logged
     */
    private synchronized <T extends DataObject> void addToDataStore(final InstanceIdentifier<T> id, final T data,
            final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, id, data);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Path Manager: {} has been published in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Path Manager: Cannot write {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Update Managed Node or TE Path components to the Data Store.
     *
     * @param <T>   As a generic method, T must be a Managed Node or a TE Path.
     * @param id    Instance Identifier of the Data Object
     * @param data  Data Object (Managed Node or TE Path)
     * @param info  Information to be logged
     */
    private synchronized <T extends DataObject> void updateToDataStore(final InstanceIdentifier<T> id, final T data,
            final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        trans.merge(LogicalDatastoreType.OPERATIONAL, id, data);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Path Manager: {} has been updated in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Path Manager: Cannot update {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Remove Managed Node or TE Path components from the Data Store.
     *
     * @param <T>  As a generic method, T must be a Managed Node or a TE Path.
     * @param id   Instance Identifier of the Data Object
     * @param info Information to be logged
     */
    private synchronized <T extends DataObject> void removeFromDataStore(final InstanceIdentifier<T> id,
            final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, id);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Path Manager: {} has been deleted in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Path Manager: Cannot delete {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain transactionChain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Path Manager Provider for {} failed in transaction: {} ", MANAGED_PATH_IDENTIFIER,
                transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain transactionChain) {
        LOG.info("Path Manager Provider for {} shut down", MANAGED_PATH_IDENTIFIER);
    }

    /**
     * Add/Update TE Path to existing Managed Node.
     *
     * @param id      Managed Node ID where the TE Path will be enforced
     * @param tePath  TE Path to be inserted/updated in the Managed Node
     */
    private TePath setupTePath(final NodeId id, TePath tePath) {
        LOG.info("Create TE Path {} for Node {}", tePath.getName(), id);
        /* Complete the TePath with Computed Route */
        TePathBuilder builder = new TePathBuilder(tePath).setStatus(PathStatus.Configured);
        final PathComputationImpl pathComputation = (PathComputationImpl) this.pceServerProvider.getPathComputation();
        if (pathComputation != null) {
            builder.setActualPath(pathComputation.computePath(tePath));
        } else {
            builder.setActualPath(new ActualPathBuilder().setStatus(OperationalStatus.Down).build());
        }

        /* Store this new TE Path for this Node ID */
        final TePath newPath = builder.build();
        tePaths.put(id, newPath.key(), newPath);

        /* Finally, setup Path on PCC if it is synchronized and if we computed a valid path */
        if (operNodes.get(id).getStatus() == NodeStatus.Sync && newPath.getActualPath().getEro() != null) {
            final ListenableFuture<RpcResult<AddLspOutput>> enforce = this.ntps
                    .addLsp(PathManagerUtils.getAddLspInput(id, newPath));
            Futures.addCallback(enforce, new FutureCallback<RpcResult<AddLspOutput>>() {
                @Override
                public void onSuccess(final RpcResult<AddLspOutput> result) {
                    if (result.isSuccessful()) {
                        LOG.debug("Enforce TE Path success {}", result.getResult());
                        // updateTePathStatus(id, newPath, result.getResult());
                    } else {
                        LOG.debug("Unable to enforce TE Path {} on Node {}: Got error {}", newPath.getName(), id,
                                result.getErrors());
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.info("Failed enforce TE Path {} on Node {}", newPath.getName(), id);
                }
            }, MoreExecutors.directExecutor());
        }
        return newPath;
    }

    /**
     * Update TE Path to existing Managed Node.
     *
     * @param id       Managed Node ID where the TE Path will be updated
     * @param oldPath  Old TE Path to verify that source and destination have note been modified
     * @param tePath   TE Path to be inserted in the Managed Node
     */
    private TePath updateTePath(final NodeId id, TePath oldPath, TePath tePath) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");
        checkArgument(oldPath != null, "Provided Old TE Path is a null object");

        LOG.info("Update TE Path {} for Node {}", tePath.getName(), id);

        IntendedPathBuilder ipb = new IntendedPathBuilder();
        final IntendedPath iPath = tePath.getIntendedPath();
        final IntendedPath oPath = oldPath.getIntendedPath();

        /* Check that Source and Destination have not been modified and revert to old value instead */
        if (!iPath.getSource().equals(oPath.getSource())) {
            LOG.warn("Source IP Address {}/{} of TE Path has been modified. Revert to initial one",
                    iPath.getSource(), oPath.getSource());
            ipb.setSource(oPath.getSource());
        }
        if (!iPath.getDestination().equals(oPath.getDestination())) {
            LOG.warn("Destination IP Address {}/{} of TE Path has been modified. Revert to initial one",
                    iPath.getDestination(), oPath.getDestination());
            ipb.setDestination(oPath.getDestination());
        }
        /* Same for Routing Method: i.e. refused to change a TE Path from RSVP-TE to Segment Routing and vice versa */
        if (!iPath.getRoutingMethod().equals(oPath.getRoutingMethod())) {
            LOG.warn("Routing Method {}/{} of TE Path has been modified. Revert to initial one",
                    iPath.getRoutingMethod(), oPath.getRoutingMethod());
            ipb.setRoutingMethod(oPath.getRoutingMethod());
        }
        TePathBuilder tpb = new TePathBuilder(tePath).setIntendedPath(ipb.build()).setStatus(PathStatus.Updated);

        /* Complete the TePath with Computed Route */
        final PathComputationImpl pathComputation = (PathComputationImpl) this.pceServerProvider.getPathComputation();
        if (pathComputation != null) {
            tpb.setActualPath(pathComputation.computePath(tePath));
        } else {
            tpb.setActualPath(new ActualPathBuilder().setStatus(OperationalStatus.Down).build());
        }

        /* Update the new TE Path for this Node ID */
        final TePath newPath = tpb.build();
        tePaths.remove(id, tePath.key());
        tePaths.put(id, newPath.key(), newPath);

        /* Finally, update Path on PCC if it is synchronized and we computed a valid path */
        if (operNodes.get(id).getStatus() == NodeStatus.Sync && newPath.getActualPath().getEro() != null) {
            final ListenableFuture<RpcResult<UpdateLspOutput>> enforce = this.ntps
                    .updateLsp(PathManagerUtils.getUpdateLspInput(id, newPath));
            Futures.addCallback(enforce, new FutureCallback<RpcResult<UpdateLspOutput>>() {
                @Override
                public void onSuccess(final RpcResult<UpdateLspOutput> result) {
                    if (result.isSuccessful()) {
                        LOG.debug("Update TE Path success {}", result.getResult());
                        // updateTePathStatus(id, newPath, result.getResult());
                    } else {
                        LOG.debug("Unable to update TE Path {} on Node {}: Got error {}", newPath.getName(), id,
                                result.getErrors());
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.info("Failed update TE Path {} on Node {}", newPath.getName(), id);
                }
            }, MoreExecutors.directExecutor());
        }
        return newPath;
    }

    @Override
    public TePath addTePath(final NodeId id, TePath tePath) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");

        /* Check that Managed Node is registered */
        if (operNodes.get(id) == null) {
            LOG.warn("Managed Node is not registered. Cancel transaction!");
            return null;
        }

        /* Check if TE Path already exist or not */
        TePath orig = tePaths.get(id, tePath.key());
        TePath newPath;
        if (orig != null) {
            newPath = updateTePath(id, orig, tePath);
            updateToDataStore(getTePathInstanceIdentifier(id, newPath), newPath, "TE Path(" + newPath.getName() + ")");
        } else {
            newPath = setupTePath(id, tePath);
            addToDataStore(getTePathInstanceIdentifier(id, newPath), newPath, "TE Path(" + newPath.getName() + ")");
        }

        return newPath;
    }

    /**
     * Remove TE Path to existing Managed Node. This method is called when a TE Path is deleted.
     *
     * @param id   Managed Node ID where the TE Path is stored
     * @param key  TE Path, as Key, to be removed
     */
    private void removeTePath(final NodeId id, final TePathKey key) {
        LOG.info("Remove TE Path {} for Node {}", key, id);

        /* Get corresponding TE Path from the Table Map */
        TePath tePath = tePaths.get(id, key);
        if (tePath == null) {
            LOG.warn("Doesn't found TE Path {} for Managed Node {}. Abort delete operation", key, id);
            return;
        }

        /*
         * Delete TE Path on PCC node if it is synchronized and TE Path is Initiated.
         * TE Path will be removed from Data Store once received the PcReport.
         */
        if (operNodes.get(id).getStatus() == NodeStatus.Sync
                && tePath.getActualPath().getType() != PathType.Initiated) {
            final ListenableFuture<RpcResult<RemoveLspOutput>> enforce = this.ntps
                    .removeLsp(new RemoveLspInputBuilder().setNode(id).setName(tePath.getName()).build());
            Futures.addCallback(enforce, new FutureCallback<RpcResult<RemoveLspOutput>>() {
                @Override
                public void onSuccess(final RpcResult<RemoveLspOutput> result) {
                    if (result.isSuccessful()) {
                        LOG.debug("Delete TE Path success {}", result.getResult());
                    } else {
                        LOG.debug("Unable to delete TE Path {} on Node {}: Got error {}", tePath.getName(), id,
                                result.getErrors());
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.info("Failed delete TE Path {} on Node {}", tePath.getName(), id);
                }
            }, MoreExecutors.directExecutor());
        } else {
            // PCC is not connected, remove the TE Path now
            unregisterTePath(id, key);
        }
    }

    @Override
    public void deleteTePath(final NodeId id, final TePathKey key) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(key != null, "Provided TE Path Key is a null object");

        TePath tePath = tePaths.get(id, key);
        if (tePath != null) {
            /*
             * Start by sending corresponding Message to PCC is TE Path is initiated.
             * TE Path will be removed when PCC confirm the deletion with PcReport.
             * If TE Path is not initiated, the TE Path should be removed by the PCC
             * by sending appropriate PcReport which is handle in unregisterTePath.
             */
            if (tePath.getActualPath().getType() == PathType.Initiated) {
                removeTePath(id, key);
            }
        } else {
            LOG.warn("TE Path {} for Node {} doesn't exist", key, id);
        }
    }

    /**
     * Compare TE Path reported by the PCC and current TE Path store in the Path Manager and apply update if needed.
     *
     * @param rptPath   Reported TE Path
     * @param curPath   Current TE Path
     *
     * @return  Updated TE Path
     */
    private TePath syncTePath(final NodeId id, final TePath rptPath, final TePath curPath) {
        // TODO: To be finished.

        /* Check if TE Path is not already synchronized */
        if (curPath.getStatus() == PathStatus.Sync) {
            if (curPath.getActualPath().getStatus() != rptPath.getActualPath().getStatus()) {
                /* Update new Operation Status of the TE Path */
                return updateTePath(id, curPath, rptPath);
            }
            LOG.debug("Reported LSP is already synchronised with TE Path");
            return curPath;
        }

        /* Compare TE Paths and update them if needed */
        // TODO: Object equals method is to strict. Need to write a more intelligent comparison method.
        /*
         * We need to check if:
         *  - Current TE Path is already synchronized
         *  - Operational Status need to be updated
         *  - TE Path needs to be updated when:
         *    - Reported ERO doesn't match Computed Path e.g. there is no ERO
         *    - Constraints parameter differs i.e. Metrics and/or Bandwidth
         *  - Source and Destination differs i.e. there is a problem between configured TE Path on PCE and PCC
         */
        if (!curPath.equals(rptPath)) {
            LOG.debug("Reported LSP is not synchronised with TE Path. Update LSP");
            return updateTePath(id, curPath, rptPath);
        }
        return curPath;
    }

    /**
     * Register Reported LSP as a TE Path for the PCC identified by its Node ID.
     *
     * @param id    Node ID of the Managed Node (PCC) which report this LSP
     * @param rl    Reported LSP
     */
    public void registerTePath(NodeId id, ReportedLsp rl) {
        LOG.info("Synchronized LSP {} for Node {}", rl, id);
        final TePath tePath = PathManagerUtils.getTePath(rl);
        if (tePath == null) {
            LOG.warn("Could not managed Reported LSP {} as a TE Path. Skip it!", rl.getName());
            return;
        }

        /* Look for existing corresponding TE Path */
        final TePath curPath = tePaths.get(id, new TePathKey(rl.getName()));

        /* Check if PCC that report this LSP is Synchronized or not */
        if (operNodes.get(id).getStatus() == NodeStatus.Sync) {
            syncTePath(id, curPath, tePath);
        } else {
            /* Store this new TePath in Managed Node for latter synchronization */
            Map<TePathKey, TePath> newPaths = operNodes.get(id).getTePath();
            newPaths.put(tePath.key(), tePath);
            ManagedNode node = new ManagedNodeBuilder()
                    .setNodeId(id)
                    .setTePath(newPaths)
                    .setStatus(operNodes.get(id).getStatus())
                    .build();
            operNodes.replace(id, node);
        }
    }

    /**
     * Remove TE Path from Operational Data Store and Path Manager.
     *
     * @param id    Node ID of the Managed Node which own this TE Path
     * @param key   TE Path name
     */
    public void unregisterTePath(final NodeId id, final TePathKey key) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(key != null, "Provided TE Path Key is a null object");

        /* Get Corresponding TE Path */
        TePath tePath = tePaths.remove(id, key);
        if (tePath != null) {
            /* Remove TE Path from Operation Datastore */
            LOG.debug("Delete TE Path {} for Node {} in DataStore", tePath.getName(), id);
            removeFromDataStore(getTePathInstanceIdentifier(id, tePath), "TE Path(" + tePath.getName() + ")");
        } else {
            LOG.warn("TE Path {} for Node {} doesn't exist", key, id);
        }
    }

    @Override
    public ManagedNode getManagedNode(NodeId id) {
        return operNodes.get(id);
    }

    @Override
    public ManagedNode addManagedNode(final ManagedNode node) {
        checkArgument(node != null, "Provided Managed Node is a null object");
        ManagedNode newNode;

        /* Create all TE Paths for this Managed Node */
        LOG.info("Create Managed Node {}", node);
        if (node.getTePath() != null) {
            final Map<TePathKey, TePath> paths = new HashMap<>();
            for (TePath tePath: node.getTePath().values()) {
                final TePath newPath = setupTePath(node.getNodeId(), tePath);
                paths.put(newPath.key(), newPath);
            }
            newNode = new ManagedNodeBuilder()
                    .setNodeId(node.getNodeId())
                    .setTePath(paths)
                    .setStatus(NodeStatus.Configured)
                    .build();
        } else {
            newNode = node;
        }

        /* Add new Managed Node to DataStore */
        addToDataStore(getManagedNodeInstanceIdentifier(newNode.getNodeId()), newNode,
                "Managed Node(" + node.getNodeId().getValue() + ")");
        operNodes.put(newNode.getNodeId(), newNode);

        return newNode;
    }

    public ManagedNode createManagedNode(final NodeId id) {
        checkArgument(id != null, "Provided Managed Node ID is a null object");

        /* Create new Managed Node if not already exist */
        ManagedNode node = operNodes.get(id);
        if (node == null) {
            node = new ManagedNodeBuilder().setNodeId(id).setStatus(NodeStatus.Enabled).build();
            operNodes.put(id, node);
            addToDataStore(getManagedNodeInstanceIdentifier(id), node, "Managed Node(" + id.getValue() + ")");
            LOG.debug("Created new Managed Node for PCC {}", id);
        } else {
            node = new ManagedNodeBuilder(operNodes.get(id)).setStatus(NodeStatus.Enabled).build();
            operNodes.replace(id, node);
            updateToDataStore(getManagedNodeInstanceIdentifier(id), node, "Managed Node(" + id.getValue() + ")");
        }
        return node;
    }

    public void syncManagedNode(NodeId id) {
        checkArgument(id != null, "Provided Managed Node ID is a null object");

        final ManagedNode node = operNodes.get(id);
        if (node == null) {
            LOG.warn("There is no Managed Node entry for this PCC {}", id);
            return;
        }

        /*
         * PCC is synchronized, browse all TE Path to check if: some are missing
         * i.e. apply previously initiated path that has been created before the PCC connects
         * and some need update i.e. apply the previous modifications
         */
        Map<TePathKey, TePath> paths = new HashMap<>();
        for (TePath path: node.getTePath().values()) {
            final TePath curPath = tePaths.get(id, path.key());
            syncTePath(id, curPath, path);
        }

        /* Store Managed Node with synchronous TePath in the DataStore */
        final ManagedNode newNode = new ManagedNodeBuilder()
                .setNodeId(id)
                .setStatus(NodeStatus.Sync)
                .setTePath(paths)
                .build();
        operNodes.replace(id, newNode);
        // TODO: TE Path has been already store with syncTePath. Here we just need to update the NodeStatus to Sync.
        updateToDataStore(getManagedNodeInstanceIdentifier(id), newNode, "Managed Node(" + id.getValue() + ")");
    }

    @Override
    public void deleteManagedNode(final ManagedNode node) {
        checkArgument(node != null, "Provided Node is a null object");

        /* Remove all associated TE Paths that are managed by the PCE */
        for (TePath tePath: node.getTePath().values()) {
            if (tePath.getActualPath().getType() == PathType.Initiated) {
                removeTePath(node.getNodeId(), tePath.key());
            }
        }

        /* Remove Managed Node from the DataStore if it is not connected */
        final ManagedNode operNode = operNodes.get(node.getNodeId());
        if (operNode.getStatus() == NodeStatus.Disabled || operNode.getStatus() == NodeStatus.Configured) {
            removeFromDataStore(getManagedNodeInstanceIdentifier(node.getNodeId()),
                    "Managed Node(" + node.getNodeId().getValue() + ")");
            operNodes.remove(node.getNodeId());
        } else {
            LOG.warn("Node {} is still connected. Keep Node in Operational Data Store.", node.getNodeId());
        }
    }

    /**
     * Call when a PCC disconnect from the PCE to disable the corresponding Manager Node.
     *
     * @param id    Managed Node ID
     */
    public void disableManagedNode(final NodeId id) {
        checkArgument(id != null, "Provided Node ID is a null object");

        /* Remove associated TE Paths that are not managed by the PCE i.e. TE Path which are not delegated */
        for (TePath tePath: tePaths.row(id).values()) {
            if (tePath.getActualPath().getType() == PathType.Stateless
                    || tePath.getActualPath().getType() == PathType.Pcc) {
                tePaths.remove(id, tePath.key());
            }
        }

        /* Replace Managed Node to the DataStore */
        final ManagedNode newNode = new ManagedNodeBuilder()
                .setNodeId(id)
                .setTePath(tePaths.row(id))
                .setStatus(NodeStatus.Disabled)
                .build();
        addToDataStore(getManagedNodeInstanceIdentifier(id), newNode, "Managed Node(" + id.getValue() + ")");
        operNodes.replace(id, newNode);
    }
}
