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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.configured.lsp.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.configured.lsp.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.configured.lsp.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
    private final InstanceIdentifier<Topology> pcepTopology;
    private final DataBroker dataBroker;
    private final PceServerProvider pceServerProvider;
    private final NetworkTopologyPcepService ntps;
    private TransactionChain chain = null;

    private final Map<NodeId, ManagedTeNode> mngNodes = new HashMap<NodeId, ManagedTeNode>();

    public PathManagerProvider(final DataBroker dataBroker, KeyedInstanceIdentifier<Topology, TopologyKey> topology,
            final NetworkTopologyPcepService ntps, final PceServerProvider pceServerProvider) {
        this.dataBroker = requireNonNull(dataBroker);
        this.pceServerProvider = requireNonNull(pceServerProvider);
        this.ntps = requireNonNull(ntps);
        this.pcepTopology = requireNonNull(topology);
        initTransactionChain();
        LOG.info("Path Manager Server started for topology {}", topology.getKey().getTopologyId().getValue());
    }

    /**
     * Remove the Path Manager Server and destroy the transaction chain.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
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
        LOG.error("Path Manager Provider for {} failed in transaction: {} ", pcepTopology,
                transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain transactionChain) {
        LOG.info("Path Manager Provider for {} shut down", pcepTopology);
    }

    /**
     * Setup Managed TE Path to existing Managed Node.
     *
     * @param id        Managed Node ID where the TE Path will be enforced
     * @param lsp    TE Path to be inserted in the Managed Node
     *
     * @return          Newly created Managed TE Path
     */
    private ManagedTePath addManagedTePath(final ManagedTeNode teNode, final ConfiguredLsp lsp) {
        checkArgument(teNode != null, "Provided Managed TE Node is a null object");
        checkArgument(lsp != null, "Provided TE Path is a null object");

        LOG.info("Setup TE Path {} for Node {}", lsp.getName(), teNode.getId());

        /* Complete the LSP with the Computed Route */
        ConfiguredLspBuilder clb = new ConfiguredLspBuilder(lsp)
                .setPathStatus(PathStatus.Configured);
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        if (pci != null) {
            clb.setComputedPath(pci.computeTePath(lsp.getIntendedPath()));
        } else {
            clb.setComputedPath(new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build());
        }

        /* Create Corresponding Managed LSP */
        final ManagedTePath mngLsp = new ManagedTePath(teNode, clb.build(), pcepTopology).setType(PathType.Initiated);

        /* Store this new Managed TE Node */
        teNode.addManagedTePath(mngLsp);

        /* Then, setup Path on PCC if it is synchronized */
        if (teNode.isSync()) {
            mngLsp.addPath(ntps);
        }

        LOG.debug("Added new Managed LSP: {}", mngLsp);
        return mngLsp;
    }

    /**
     * Update TE Path to existing Managed Node.
     *
     * @param id       Managed Node ID where the TE Path will be updated
     * @param mngPath  Managed TE Path to be updated
     * @param tePath   New TE Path to be updated in the Managed Node
     */
    private ConfiguredLsp updateManagedTePath(final ManagedTePath mngPath, final ConfiguredLsp tePath) {
        checkArgument(mngPath != null, "Provided Managed TE Path is a null object");
        checkArgument(tePath != null, "Provided TE Path is a null object");

        final ManagedTeNode teNode = mngPath.getManagedTeNode();
        final IntendedPath iPath = tePath.getIntendedPath();
        final IntendedPath oPath = mngPath.getLsp().getIntendedPath();
        IntendedPathBuilder ipb = new IntendedPathBuilder(iPath);

        LOG.info("Update TE Path {} for Node {}", mngPath.getLsp().getName(), teNode.getId());

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
        ConfiguredLspBuilder clb = new ConfiguredLspBuilder(tePath)
                .setIntendedPath(ipb.build())
                .setPathStatus(PathStatus.Updated);
        /* Complete it with the new Computed Route */
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        if (pci != null) {
            clb.setComputedPath(pci.computeTePath(tePath.getIntendedPath()));
        } else {
            clb.setComputedPath(new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build());
        }

        /* Finally, update the new TE Path for this Node ID */
        mngPath.setConfiguredLsp(clb.build());
        mngPath.updateToDataStore();

        /* Finally, update Path on PCC if it is synchronized and we computed a valid path */
        if (teNode.isSync()) {
            mngPath.updatePath(ntps);
        }

        LOG.debug("Updated Managed Paths: {}", mngPath);
        return mngPath.getLsp();
    }

    /**
     * Create a new Managed TE Path.
     *
     * @param id        Managed TE Node Identifier to which the TE path is attached.
     * @param cfgLsp    TE Path.
     *
     * @return          new or updated TE Path i.e. original TE Path augmented by a valid computed route.
     */
    public ConfiguredLsp createManagedTePath(final NodeId id, ConfiguredLsp cfgLsp) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(cfgLsp != null, "Provided TE Path is a null object");

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return null;
        }

        /* Check if TE Path already exist or not */
        ManagedTePath tePath = teNode.getManagedTePath(cfgLsp.key());
        if (tePath != null) {
            updateManagedTePath(tePath, cfgLsp);
            tePath.updateToDataStore();
        } else {
            tePath = addManagedTePath(teNode, cfgLsp);
            tePath.addToDataStore();
        }

        return tePath.getLsp();
    }

    /**
     * Remove TE Path to existing Managed Node. This method is called when a TE Path is deleted.
     *
     * @param id   Managed Node ID where the TE Path is stored
     * @param key  TE Path, as Key, to be removed
     */
    private void removeTePath(final NodeId id, final ConfiguredLspKey key) {
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
         * Delete TE Path on PCC node if it is synchronized, TE Path is Initiated and is enforced on the PCC.
         * TE Path will be removed from Data Store once received the PcReport.
         */
        if (teNode.isSync() && mngPath.getType() == PathType.Initiated
                && mngPath.getLsp().getPathStatus() == PathStatus.Sync) {
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
    public void deleteManagedTePath(final NodeId id, final ConfiguredLspKey key) {
        checkArgument(id != null, "Provided Node ID is a null object");
        checkArgument(key != null, "Provided TE Path Key is a null object");

        /* Check that Managed Node is registered */
        final ManagedTeNode teNode = mngNodes.get(id);
        if (teNode == null) {
            LOG.warn("Managed TE Node {} is not registered. Cancel transaction!", id);
            return;
        }

        ManagedTePath mngPath = teNode.getManagedTePath(key);
        if (mngPath == null) {
            LOG.warn("Managed TE Path {} for TE Node {} doesn't exist", key, id);
            return;
        }

        /*
         * Start by sending corresponding Message to PCC if TE Path is initiated.
         * TE Path will be removed when PCC confirm the deletion with PcReport.
         * If TE Path is not initiated, the TE Path should be removed by the PCC
         * by sending appropriate PcReport which is handle in unregisterTePath.
         */
        if (teNode.isSync() && mngPath.getType() == PathType.Initiated) {
            removeTePath(id, key);
        } else {
            LOG.warn("Managed TE Path {} for TE Node {} is not managed by this PCE. Remove only configuration",
                    key, id);
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
    public ManagedTePath registerTePath(NodeId id, final ConfiguredLsp rptPath, final PathType ptype) {
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
            final ManagedTePath newPath = new ManagedTePath(teNode, pcepTopology).setType(ptype);
            final ConfiguredLspBuilder clb = new ConfiguredLspBuilder(rptPath);

            /* Check if ERO needs to be updated i.e. Path Description is empty */
            if (rptPath.getComputedPath().getPathDescription() == null) {
                clb.setPathStatus(PathStatus.Updated);
                /* Complete the TE Path with Computed Route */
                final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
                if (pci != null) {
                    clb.setComputedPath(pci.computeTePath(rptPath.getIntendedPath()));
                } else {
                    clb.setComputedPath(
                            new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build());
                }

                /* Finally, update the new TE Path for this Node ID */
                newPath.setConfiguredLsp(clb.build());

                /* and update Path on PCC if it is synchronized */
                if (teNode.isSync()) {
                    newPath.updatePath(ntps);
                }
            } else {
                /* Mark this TE Path as Synchronous and add it to the Managed TE Path */
                clb.setPathStatus(PathStatus.Sync);
                newPath.setConfiguredLsp(clb.build());
            }

            /* Store this new reported TE Path */
            teNode.addManagedTePath(newPath);

            LOG.debug("Created new Managed TE Path: {}", newPath);
            return newPath;
        }

        /* Check this TE Path against current configuration */
        final PathStatus newStatus = curPath.checkReportedPath(rptPath);
        LOG.debug("Managed TE Path {} got new status {}", curPath.getLsp().getName(), newStatus);

        /* Check if we should stop here. i.e. the Path is failed */
        if (newStatus == PathStatus.Failed) {
            curPath.setConfiguredLsp(new ConfiguredLspBuilder(rptPath).setPathStatus(PathStatus.Failed).build());
            curPath.updateToDataStore();
            LOG.debug("Managed TE Path {} is in Failure", curPath);
            return curPath;
        }

        /* Check if Current Path has no valid route while Reported Path has one */
        if ((curPath.getLsp().getComputedPath().getPathDescription() == null)
                && (rptPath.getComputedPath().getPathDescription() != null)) {
            curPath.setConfiguredLsp(new ConfiguredLspBuilder(rptPath).setPathStatus(PathStatus.Sync).build());
            curPath.updateToDataStore();
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
        if (newStatus == PathStatus.Sync && curPath.getLsp().getPathStatus() != PathStatus.Sync) {
            curPath.sync();
            LOG.debug("Sync Managed TE Path {} on NodeId {}", curPath, id);
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
    public void unregisterTePath(final NodeId id, final ConfiguredLspKey key) {
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
    public void setTePathFailed(final NodeId id, final ConfiguredLspKey key) {
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
     * @param nodeId  Managed TE Node Identifier
     * @param pccNode Path Computation Client
     *
     * @return        New Managed TE Node.
     */
    public synchronized ManagedTeNode createManagedTeNode(final NodeId nodeId, final PcepNodeConfig pccNode) {
        checkArgument(pccNode != null, "Provided Managed TE Node is a null object");

        /* First, create new Managed TE Node */
        ManagedTeNode teNode = new ManagedTeNode(nodeId, chain);
        mngNodes.put(nodeId, teNode);

        /* Then, create all TE Paths for this Managed Node */
        if (pccNode.getConfiguredLsp() != null) {
            for (ConfiguredLsp tePath: pccNode.getConfiguredLsp().values()) {
                addManagedTePath(teNode, tePath);
            }
        }

        LOG.info("Created new Managed TE Node {}", nodeId);

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
            switch (mngPath.getLsp().getPathStatus()) {
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
     * @param nodeId  Managed Node Identifier
     */
    public void deleteManagedTeNode(final NodeId nodeId) {
        checkArgument(nodeId != null, "Provided Node Identifie is a null object");

        /* Verify that Node is managed by the PCE Server */
        final ManagedTeNode teNode = mngNodes.get(nodeId);
        if (teNode == null) {
            LOG.warn("Unknown Managed TE Node {}. Abort!", nodeId);
            return;
        }

        /* Remove all associated TE Paths that are managed by the PCE */
        for (ManagedTePath mngPath: teNode.getTePaths().values()) {
            if (mngPath.getType() == PathType.Initiated) {
                removeTePath(nodeId, mngPath.getLsp().key());
            }
        }

        /* Remove Managed Node from PCE Server if it is not connected */
        if (!teNode.isSync()) {
            mngNodes.remove(nodeId);
        } else {
            LOG.warn("Node {} is still connected. Keep Node in PCE Server.", nodeId);
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
