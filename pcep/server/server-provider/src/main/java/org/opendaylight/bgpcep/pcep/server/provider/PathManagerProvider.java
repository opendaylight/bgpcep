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

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.ActualPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements the Path Manager in charge of Managed TE Node and Managed TE Path.
 *
 * @author Olivier Dugeon
 */

public final class PathManagerProvider implements TransactionChainListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerProvider.class);
    private static final InstanceIdentifier<ManagedPath> MANAGED_PATH_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).build();

    private final DataBroker dataBroker;
    private final PceServerProvider pceServerProvider;
    private final NetworkTopologyPcepService ntps;
    private TransactionChain chain = null;

    private final Map<NodeId, ManagedTeNode> mngNodes = new HashMap<NodeId, ManagedTeNode>();

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
        this.chain = dataBroker.createMergingTransactionChain(this);
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
     * Setup Managed TE Path to existing Managed Node.
     *
     * @param id        Managed Node ID where the TE Path will be enforced
     * @param tePath    TE Path to be inserted in the Managed Node
     *
     * @return          Newly created Managed TE Path
     */
    private ManagedTePath addManagedTePath(final ManagedTeNode teNode, final TePath tePath) {
        checkArgument(teNode != null, "Provided Managed TE Node is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");

        LOG.info("Setup TE Path {} for Node {}", tePath.getName(), teNode.getId());

        /* Complete the TePath with the Computed Route */
        TePathBuilder tpBuilder = new TePathBuilder(tePath)
                .setPathType(PathType.Initiated)
                .setPathStatus(PathStatus.Configured);
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        if (pci != null) {
            tpBuilder.setActualPath(pci.computeTePath(tePath.getIntendedPath()));
        } else {
            tpBuilder.setActualPath(new ActualPathBuilder().setStatus(OperationalStatus.Down).build());
        }

        /* Create Corresponding Managed TE Path */
        final ManagedTePath mngPath = new ManagedTePath(teNode, tpBuilder.build());

        /* Store this new Managed TE Node */
        teNode.addManagedTePath(mngPath);

        /* Then, setup Path on PCC if it is synchronized */
        if (teNode.isSync()) {
            mngPath.addPath(ntps);
        }

        LOG.debug("Added new Managed TE Paths: {}", mngPath);
        return mngPath;
    }

    /**
     * Update TE Path to existing Managed Node.
     *
     * @param id       Managed Node ID where the TE Path will be updated
     * @param mngPath  Managed TE Path to be updated
     * @param tePath   New TE Path to be updated in the Managed Node
     */
    private TePath updateManagedTePath(final ManagedTePath mngPath, final TePath tePath) {
        checkArgument(mngPath != null, "Provided Managed TE Path is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");

        final ManagedTeNode teNode = mngPath.getManagedTeNode();
        final IntendedPath iPath = tePath.getIntendedPath();
        final IntendedPath oPath = mngPath.getPath().getIntendedPath();
        IntendedPathBuilder ipb = new IntendedPathBuilder(iPath);

        LOG.info("Update TE Path {} for Node {}", mngPath.getPath().getName(), teNode.getId());

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
        /*
         * Same for Routing Method: i.e. refused to change a TE Path from
         * RSVP-TE to Segment Routing and vice versa
         */
        if (!iPath.getRoutingMethod().equals(oPath.getRoutingMethod())) {
            LOG.warn("Routing Method {}/{} of TE Path has been modified. Revert to initial one",
                    iPath.getRoutingMethod(), oPath.getRoutingMethod());
            ipb.setRoutingMethod(oPath.getRoutingMethod());
        }

        /* Create updated TE Path */
        TePathBuilder tpb = new TePathBuilder(tePath)
                .setIntendedPath(ipb.build())
                .setPathStatus(PathStatus.Updated)
                .setPathType(mngPath.getPath().getPathType());
        /* Complete it with the new Computed Route */
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        if (pci != null) {
            tpb.setActualPath(pci.computeTePath(tePath.getIntendedPath()));
        } else {
            tpb.setActualPath(new ActualPathBuilder().setStatus(OperationalStatus.Down).build());
        }

        /* Finally, update the new TE Path for this Node ID */
        mngPath.setPath(tpb.build());

        /* Finally, update Path on PCC if it is synchronized and we computed a valid path */
        if (teNode.isSync()) {
            mngPath.updatePath(ntps);
        }

        LOG.debug("Updated Managed Paths: {}", mngPath);
        return mngPath.getPath();
    }

    /**
     * Create a new Managed TE Path.
     *
     * @param id        Managed TE Node Identifier to which the TE path is attached.
     * @param tePath    TE Path.
     *
     * @return          new or updated TE Path i.e. original TE Path augmented by a valid computed route.
     */
    public TePath createManagedTePath(final NodeId id, TePath tePath) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return null;
        }

        /* Check if TE Path already exist or not */
        ManagedTePath orig = teNode.getManagedTePath(tePath.key());
        if (orig != null) {
            updateManagedTePath(orig, tePath);
            orig.updateToDataStore();
        } else {
            orig = addManagedTePath(teNode, tePath);
            orig.addToDataStore();
        }

        return orig.getPath();
    }

    /**
     * Remove TE Path to existing Managed Node. This method is called when a TE Path is deleted.
     *
     * @param id   Managed Node ID where the TE Path is stored
     * @param key  TE Path, as Key, to be removed
     */
    private void removeTePath(final NodeId id, final TePathKey key) {
        LOG.info("Remove TE Path {} for Node {}", key, id);

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return;
        }

        /* Get corresponding TE Path from the TE Node */
        ManagedTePath mngPath = teNode.getManagedTePath(key);
        if (mngPath == null) {
            LOG.warn("Doesn't found Managed TE Path {} for TE Node {}. Abort delete operation", key, id);
            return;
        }

        /*
         * Delete TE Path on PCC node if it is synchronized and TE Path is Initiated.
         * TE Path will be removed from Data Store once received the PcReport.
         */
        if (teNode.isSync() && mngPath.getPath().getPathType() == PathType.Initiated) {
            mngPath.removePath(ntps);
        }

        /*
         * If TE Path is not Initiated or there is a failure to remove it on PCC,
         * remove immediately TE Path from the Data Store.
         */
        if (!mngPath.isSent()) {
            unregisterTePath(id, key);
        }
    }

    /**
     * Remove TE Path to existing Managed Node if TE Path has been initiated by the PCE server.
     *
     * @param id   Managed Node ID where the TE Path is stored
     * @param key  TE Path, as Key, to be removed
     */
    public void deleteManagedTePath(final NodeId id, final TePathKey key) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(key != null, "Provided TE Path Key is a null object");

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return;
        }

        ManagedTePath mngPath = teNode.getManagedTePath(key);
        if (mngPath != null) {
            /*
             * Start by sending corresponding Message to PCC if TE Path is initiated.
             * TE Path will be removed when PCC confirm the deletion with PcReport.
             * If TE Path is not initiated, the TE Path should be removed by the PCC
             * by sending appropriate PcReport which is handle in unregisterTePath.
             */
            if (teNode.isSync() && mngPath.getPath().getPathType() == PathType.Initiated) {
                removeTePath(id, key);
            } else {
                LOG.warn("Managed TE Path {} for TE Node {} is not managed by this PCE. Remove only configuration",
                        key, id);
            }
        } else {
            LOG.warn("Managed TE Path {} for TE Node {} doesn't exist", key, id);
        }
    }

    /**
     * Register Reported LSP as a TE Path for the PCC identified by its Node ID.
     *
     * @param id        Node ID of the Managed Node (PCC) which report this LSP
     * @param rptPath   Reported TE Path
     *
     * @return          Newly created or Updated Managed TE Path
     */
    public ManagedTePath registerTePath(NodeId id, final TePath rptPath) {
        checkArgument(id != null, "Provided Node ID is a null object");

        /* Verify we got a valid reported TE Path */
        if (rptPath == null) {
            return null;
        }

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return null;
        }

        LOG.info("Registered TE Path {} for Node {}", rptPath, id);

        /* Look for existing corresponding Managed TE Path */
        final ManagedTePath curPath = teNode.getManagedTePath(rptPath.key());

        if (curPath == null) {
            final ManagedTePath newPath = new ManagedTePath(teNode);
            final TePathBuilder tpb = new TePathBuilder(rptPath);

            /* Check if ERO needs to be updated i.e. Path Description is empty */
            if (rptPath.getActualPath().getPathDescription() == null) {
                tpb.setPathStatus(PathStatus.Updated);
                /* Complete the TE Path with Computed Route */
                final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
                if (pci != null) {
                    tpb.setActualPath(pci.computeTePath(rptPath.getIntendedPath()));
                } else {
                    tpb.setActualPath(new ActualPathBuilder().setStatus(OperationalStatus.Down).build());
                }

                /* Finally, update the new TE Path for this Node ID */
                newPath.setPath(tpb.build());

                /* and update Path on PCC if it is synchronized */
                if (teNode.isSync()) {
                    newPath.updatePath(ntps);
                }
            } else {
                /* Mark this TE Path as Synchronous and add it to the Managed TE Path */
                tpb.setPathStatus(PathStatus.Sync);
                newPath.setPath(tpb.build());
            }

            /* Store this new reported TE Path */
            teNode.addManagedTePath(newPath);
            newPath.addToDataStore();

            LOG.debug("Created new Managed TE Path: {}", newPath);
            return newPath;
        }

        /* Check this TE Path against current configuration */
        final PathStatus newStatus = curPath.checkReportedPath(rptPath);
        LOG.debug("Managed TE Path {} got new status {}", curPath.getPath().getName(), newStatus);
        /* Check if we should stop here. i.e. the Path is failed */
        if (newStatus == PathStatus.Failed) {
            curPath.setPath(new TePathBuilder(rptPath).setPathStatus(PathStatus.Failed).build());
            LOG.debug("Managed TE Path {} is in Failure", curPath);
            return curPath;
        }
        /* Check if Current Path has no valid route while Reported Path has one */
        if ((curPath.getPath().getActualPath().getPathDescription() == null)
                && (rptPath.getActualPath().getPathDescription() != null)) {
            curPath.setPath(new TePathBuilder(rptPath).setPathStatus(PathStatus.Sync).build());
            LOG.debug("Updated Managed TE Path with reported LSP: {}", curPath);
            return curPath;
        }
        /* Check if we need to update the TE Path */
        if (teNode.isSync() && newStatus == PathStatus.Updated) {
            curPath.updatePath(ntps);
            LOG.debug("Updated Managed TE Path {} on NodeId {}", curPath, id);
            return curPath;
        }
        /* Check if TE Path becoming in SYNC */
        if (newStatus == PathStatus.Sync && curPath.getPath().getPathStatus() != PathStatus.Sync) {
            curPath.sync(rptPath.getActualPath().getStatus());
            LOG.debug("Sync Managed TE Path {} on NodeId {}", curPath, id);
            return curPath;
        }
        /* Check if Reported LSP status has changed */
        if (!curPath.getPath().getActualPath().getStatus().equals(rptPath.getActualPath().getStatus())) {
            curPath.sync(rptPath.getActualPath().getStatus());
            LOG.debug("Update Operation Status for Managed TE Path {} on NodeId {}", curPath, id);
            return curPath;
        }

        /* Managed Path is already in SYNC, nothing to do */
        return curPath;
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

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("There is no Managed TE Node entry for this PCC {}", id);
            return;
        }

        teNode.removeManagedTePath(key);
    }

    /**
     * Indicate that the TE Path is failed following reception of a PCE Error message.
     *
     * @param id    Node ID of the Managed Node which own this TE Path
     * @param key   TE Path name
     */
    public void setTePathFailed(final NodeId id, final TePathKey key) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(key != null, "Provided TE Path Key is a null object");

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("There is no Managed TE Node entry for this PCC {}", id);
            return;
        }

        /* Get Corresponding TE Path */
        ManagedTePath mngPath = teNode.getManagedTePath(key);
        if (mngPath != null) {
            mngPath.failed();
        } else {
            LOG.warn("TE Path {} for Node {} doesn't exist", key, id);
        }
    }

    /**
     * Check if a Managed TE Node is controlled by the Path Manager.
     *
     * @param id    Node ID of the Managed TE Node
     *
     * @return      True if Managed TE Node exist, false otherwise
     */
    public boolean checkManagedTeNode(final NodeId id) {
        return (mngNodes.get(id) != null);
    }

    /**
     * Create new Managed TE Node. This method is called by a new Managed Node is created in the Configuration
     * Data Store. All TE Path associated to this Managed Node are also created. A new Managed Node, with TE Paths
     * augmented with valid computed routes, is stored in the Operational Data Store.
     *
     * @param node  Managed Node
     *
     * @return      New Managed TE Node.
     */
    public synchronized ManagedTeNode createManagedTeNode(final ManagedNode node) {
        checkArgument(node != null, "Provided Managed Node is a null object");

        ManagedTeNode teNode = new ManagedTeNode(node.getNodeId(), chain);

        /* Create all TE Paths for this Managed Node */
        if (node.getTePath() != null) {
            for (TePath tePath: node.getTePath().values()) {
                final ManagedTePath newPath = addManagedTePath(teNode, tePath);
                teNode.addManagedTePath(newPath);
            }
        }

        LOG.info("Created new Managed TE Node {}", node.key());

        /* Add new Managed TE Node to DataStore */
        teNode.addToDataStore();
        return teNode;
    }

    /**
     * Register a PCC as a new Managed TE Node. This method is called by the PCEP Topology Listener when a new PCC
     * connects to the PCE Server.
     *
     * @param id    Node ID of the PCC
     *
     * @return      current or new Managed TE Node
     */
    public synchronized ManagedTeNode registerManagedTeNode(final NodeId id) {
        checkArgument(id != null, "Provided Managed Node ID is a null object");

        ManagedTeNode teNode = mngNodes.get(id);
        /* Create new Managed TE Node if not already exist */
        if (teNode == null) {
            teNode = new ManagedTeNode(id, chain);
            mngNodes.put(id, teNode);
            teNode.addToDataStore();
            LOG.debug("Created new Managed TE Node: {}", teNode);
        }
        return teNode;
    }

    /**
     * Synchronized Managed TE Node. Once PCC finished initial report of all LSP, its state change to Synchronized.
     * This function update the Managed TE Node status, and then parse all reported LSPs to determine if:
     *  - There is missing LSPs that need to be setup
     *  - There is LSPs that need to be updated
     *
     * @param id    Node ID of the Managed TE Node
     */
    public void syncManagedTeNode(final NodeId id) {
        checkArgument(id != null, "Provided Managed Node ID is a null object");

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("There is no Managed TE Node entry for this PCC {}", id);
            return;
        }

        if (teNode.isSync()) {
            LOG.debug("PCC {} is already synchronised", id);
            return;
        }

        /* First, mark the Node as Synchronous */
        teNode.sync();

        /*
         * PCC is synchronized, browse all TE Path to check if:
         *  - some are missing i.e. apply previously initiated paths that have been created before the PCC connects
         *  - some need update i.e. apply previous modifications
         */
        for (ManagedTePath mngPath : teNode.getTePaths().values()) {
            switch (mngPath.getPath().getPathStatus()) {
                case Updated:
                    mngPath.updatePath(ntps);
                    break;
                case Configured:
                    mngPath.addPath(ntps);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Delete Managed TE Node. This method is called when a Managed Node is removed from the Configuration Data Store.
     * All initiated Managed TE Path own by this PCC are removed and corresponding Managed Node is removed from the
     * Operational Data Store if it is not connected.
     *
     * @param node  Managed Node
     */
    public void deleteManageTeNode(final ManagedNode node) {
        checkArgument(node != null, "Provided Node is a null object");

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(node.getNodeId());
        if (teNode == null) {
            LOG.warn("Unknown Managed TE Node {}. Abort!", node.getNodeId());
            return;
        }

        /* Remove all associated TE Paths that are managed by the PCE */
        for (ManagedTePath mngPath: teNode.getTePaths().values()) {
            if (mngPath.getPath().getPathType() == PathType.Initiated) {
                removeTePath(node.getNodeId(), mngPath.getPath().key());
            }
        }

        /* Remove Managed Node from the DataStore if it is not connected */
        if (!teNode.isSync()) {
            teNode.removeFromDataStore();
            mngNodes.remove(node.getNodeId());
        } else {
            LOG.warn("Node {} is still connected. Keep Node in Operational Data Store.", node.getNodeId());
        }
    }

    /**
     * Call when a PCC disconnect from the PCE to disable the corresponding Managed TE Node.
     *
     * @param id    Managed Node ID
     */
    public void disableManagedTeNode(final NodeId id) {
        checkArgument(id != null, "Provided Node ID is a null object");

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Unknown Managed TE Node {}. Abort!", id);
            return;
        }

        /* And mark the Node as disable */
        teNode.disable();
    }

}
