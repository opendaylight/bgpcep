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

import com.google.common.util.concurrent.FutureCallback;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedEdgeTrigger;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphTrigger;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.graph.ConnectedVertexTrigger;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.intended.path.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.RemoveLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.UpdateLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Empty;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements the Path Manager in charge of Managed TE Node and Managed TE Path.
 *
 * @author Olivier Dugeon
 */
public final class PathManagerProvider implements FutureCallback<Empty>, AutoCloseable, ConnectedGraphTrigger {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerProvider.class);

    private final Map<NodeId, ManagedTeNode> mngNodes = new HashMap<>();
    private final WithKey<Topology, TopologyKey> pcepTopology;
    private final DataBroker dataBroker;
    private final DefaultPceServerProvider pceServerProvider;
    private final AddLsp addLsp;
    private final UpdateLsp updateLsp;
    private final RemoveLsp removeLsp;
    private TransactionChain chain = null;
    private ConnectedGraph tedGraph = null;

    public PathManagerProvider(final DataBroker dataBroker,
            final WithKey<Topology, TopologyKey> topology, final RpcService rpcService,
            final DefaultPceServerProvider pceServerProvider) {
        this.dataBroker = requireNonNull(dataBroker);
        this.pceServerProvider = requireNonNull(pceServerProvider);
        addLsp = rpcService.getRpc(AddLsp.class);
        updateLsp = rpcService.getRpc(UpdateLsp.class);
        removeLsp = rpcService.getRpc(RemoveLsp.class);
        pcepTopology = requireNonNull(topology);
        initTransactionChain();
        tedGraph = getGraph();
        LOG.info("Path Manager Server started for topology {}", topology.key().getTopologyId().getValue());
    }

    /**
     * Remove the Path Manager Server and destroy the transaction chain.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        tedGraph = pceServerProvider.getTedGraph();
        if (tedGraph != null) {
            tedGraph.unRegisterTrigger(this, pcepTopology.key());
        }
        destroyTransactionChain();
    }

    private ConnectedGraph getGraph() {
        if (tedGraph == null) {
            tedGraph = pceServerProvider.getTedGraph();
            if (tedGraph != null) {
                tedGraph.registerTrigger(this, pcepTopology.key());
            }
        }
        return tedGraph;
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for Path Manager Server {}", this);
        checkState(chain == null, "Transaction chain has to be closed before being initialized");
        chain = dataBroker.createMergingTransactionChain();
        chain.addCallback(this);
    }

    /**
     * Destroy the current transaction chain.
     */
    private synchronized void destroyTransactionChain() {
        if (chain != null) {
            LOG.debug("Destroy transaction chain for Path Manager {}", this);
            chain = null;
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
    public synchronized void onFailure(final Throwable cause) {
        LOG.error("Path Manager Provider for {}", pcepTopology, cause);
    }

    @Override
    public void onSuccess(final Empty value) {
        LOG.info("Path Manager Provider for {} shut down", pcepTopology);
    }

    /**
     * Setup Managed TE Path to existing Managed Node.
     *
     * @param teNode    Managed TE Node where the TE Path will be enforced
     * @param lsp       TE Path to be inserted in the Managed Node
     *
     * @return          Newly created Managed TE Path
     */
    private ManagedTePath addManagedTePath(final ManagedTeNode teNode, final ConfiguredLsp lsp) {
        checkArgument(teNode != null, "Provided Managed TE Node is a null object");
        checkArgument(lsp != null, "Provided TE Path is a null object");

        LOG.info("Setup TE Path {} for Node {}", lsp.getName(), teNode.getId());

        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        /* Create Corresponding Managed LSP */
        final ManagedTePath mngLsp =
            new ManagedTePath(
                    teNode,
                    /* Complete the LSP with the Computed Route */
                    new ConfiguredLspBuilder(lsp)
                        .setPathStatus(PathStatus.Configured)
                        .setComputedPath(
                            pci == null
                                ? new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build()
                                : pci.computeTePath(lsp.getIntendedPath()))
                        .build(),
                    pcepTopology)
                .setType(PathType.Initiated);

        /* Store this new Managed TE Node */
        teNode.addManagedTePath(mngLsp);

        /* Then, setup Path on PCC if it is synchronized */
        if (teNode.isSync()) {
            mngLsp.addPath(addLsp);
        }

        LOG.debug("Added new Managed LSP: {}", mngLsp);
        return mngLsp;
    }

    /**
     * Update TE Path to existing Managed Node.
     *
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

        /* Same for Address Family i.e. refused to change a TE Path from RSVP-TE to Segment Routing and vice versa */
        if (!iPath.getConstraints().getAddressFamily().equals(oPath.getConstraints().getAddressFamily())) {
            LOG.warn("Address Family {}/{} of TE Path has been modified. Revert to initial one",
                    iPath.getConstraints().getAddressFamily(), oPath.getConstraints().getAddressFamily());
            ipb.setConstraints(new ConstraintsBuilder(iPath.getConstraints())
                    .setAddressFamily(oPath.getConstraints().getAddressFamily()).build());
        }

        /* Create updated TE Path */
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        mngPath.setConfiguredLsp(
            new ConfiguredLspBuilder(tePath)
                .setIntendedPath(ipb.build())
                .setPathStatus(PathStatus.Updated)
                /* Complete it with the new Computed Route */
                .setComputedPath(
                    pci == null
                        ? new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build()
                        : pci.computeTePath(tePath.getIntendedPath()))
                .build());

        /* Finally, update the new TE Path for this Node ID */
        mngPath.updateToDataStore();

        /* Finally, update Path on PCC if it is synchronized and we computed a valid path */
        if (teNode.isSync()) {
            mngPath.updatePath(updateLsp);
        }

        LOG.debug("Updated Managed Paths: {}", mngPath);
        return mngPath.getLsp();
    }


    /**
     * Update Computed Path to an existing Managed TE Path.
     *
     * @param mngPath  Managed TE Path to be updated
     */
    private void updateComputedPath(final ManagedTePath mngPath, final boolean add) {
        checkArgument(mngPath != null, "Provided Managed TE Path is a null object");

        final ManagedTeNode teNode = mngPath.getManagedTeNode();

        LOG.info("Update Computed Path for Managed TE Path {}", mngPath.getLsp().getName());

        /* Update the TE Path with the new computed path */
        final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
        mngPath.setConfiguredLsp(
            new ConfiguredLspBuilder(mngPath.getLsp())
                .setPathStatus(PathStatus.Updated)
                .setComputedPath(
                    /* Compute new Route */
                    pci == null
                        ? new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build()
                        : pci.computeTePath(mngPath.getLsp().getIntendedPath()))
                .build());

        if (add) {
            mngPath.addToDataStore();
        } else {
            mngPath.updateToDataStore();
        }

        /* Finally, update Path on PCC if it is synchronized and computed path is valid */
        if (teNode.isSync()) {
            if (add) {
                mngPath.addPath(addLsp);
            } else {
                mngPath.updatePath(updateLsp);
            }
        } else {
            mngPath.unSetTriggerFlag();
        }

        LOG.debug("Computed new path: {}", mngPath.getLsp().getComputedPath());
    }

    /**
     * Create a new Managed TE Path.
     *
     * @param id        Managed TE Node Identifier to which the TE path is attached.
     * @param cfgLsp    TE Path.
     *
     * @return          new or updated TE Path i.e. original TE Path augmented by a valid computed route.
     */
    public ConfiguredLsp createManagedTePath(final NodeId id, final ConfiguredLsp cfgLsp) {
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
            mngPath.removePath(removeLsp);
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
    public ManagedTePath registerTePath(final NodeId id, final ConfiguredLsp rptPath, final PathType ptype) {
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
            /* Check if ERO needs to be updated i.e. Path Description is empty */
            if (rptPath.getComputedPath().getPathDescription() == null) {
                /* Finally, update the new TE Path for this Node ID */
                final PathComputationImpl pci = (PathComputationImpl) pceServerProvider.getPathComputation();
                newPath.setConfiguredLsp(
                    new ConfiguredLspBuilder(rptPath)
                        .setPathStatus(PathStatus.Updated)
                        .setComputedPath(
                            /* Complete the TE Path with Computed Route */
                            pci == null
                                ? new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build()
                                : pci.computeTePath(rptPath.getIntendedPath()))
                        .build());
                /* and update Path on PCC if it is synchronized */
                if (teNode.isSync()) {
                    newPath.updatePath(updateLsp);
                }
            } else {
                /* Mark this TE Path as Synchronous and add it to the Managed TE Path */
                newPath.setConfiguredLsp(new ConfiguredLspBuilder(rptPath).setPathStatus(PathStatus.Sync).build());
            }

            /* Update Reserved Bandwidth and Add triggers in the Connected Graph */
            newPath.setGraph(getGraph());

            /* Store this new reported TE Path */
            teNode.addManagedTePath(newPath);

            LOG.debug("Created new Managed TE Path: {}", newPath);
            return newPath;
        }

        /*
         * If PCE restart, it will consider all configured TE Path as Initiated, while some of configurations
         * concern only Delegate Path. Thus, It is necessary to verify the Path Type and correct them:
         * i.e. a reported TE Path, if not Initiated, will overwrite an Initiated configured TE Path.
         */
        if (curPath.getType() == PathType.Initiated && ptype != PathType.Initiated) {
            LOG.debug("Reset Path Type to {} for Managed TE Path {}", ptype, curPath.getLsp().getName());
            curPath.setType(ptype);
        }

        /* Check this TE Path against current configuration */
        final PathStatus newStatus = curPath.checkReportedPath(rptPath);
        LOG.debug("Managed TE Path {} got new status {}", curPath.getLsp().getName(), newStatus);

        /* Check if we should stop here. i.e. the Path is failed */
        if (newStatus == PathStatus.Failed) {
            curPath.setConfiguredLsp(new ConfiguredLspBuilder(rptPath).setPathStatus(PathStatus.Failed).build());
            curPath.updateToDataStore();
            /* Path is in failure. Reset Trigger Flag to authorize future path re-computation */
            curPath.unSetTriggerFlag();
            LOG.debug("Managed TE Path {} is in Failure", curPath);
            return curPath;
        }

        /* Check if Current Path has no valid route while Reported Path has one */
        if (curPath.getLsp().getComputedPath().getPathDescription() == null
                && rptPath.getComputedPath().getPathDescription() != null) {
            curPath.setConfiguredLsp(new ConfiguredLspBuilder(rptPath).setPathStatus(PathStatus.Sync).build());
            curPath.updateGraph(getGraph());
            curPath.updateToDataStore();
            LOG.debug("Updated Managed TE Path with reported LSP: {}", curPath);
            return curPath;
        }

        /* Check if we need to update the TE Path */
        if (teNode.isSync() && newStatus == PathStatus.Updated) {
            curPath.updatePath(updateLsp);
            LOG.debug("Updated Managed TE Path {} on NodeId {}", curPath, id);
            return curPath;
        }

        /* Check if TE Path becoming in SYNC */
        if (newStatus == PathStatus.Sync && curPath.getLsp().getPathStatus() != PathStatus.Sync) {
            curPath.sync();
            curPath.updateGraph(getGraph());
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

        /* Remove the TE Path and associated Bandwidth if any */
        final ManagedTePath tePath = teNode.removeManagedTePath(key);
        if (tePath != null) {
            tePath.unsetGraph(getGraph());
        }
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
        return mngNodes.get(id) != null;
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
         * PCC is synchronized, browse all Managed TE Path to check if:
         *  - some are missing i.e. apply previously initiated paths that have been created before the PCC connects
         *  - some need update i.e. apply previous modifications
         * And for eligible Managed TE Path, give it a last chance to obtain a valid computed path. This is mandatory
         * when ODL restart: Path Manager Configuration is read before a valid TED is available.
         */
        if (teNode.getTePaths() != null && !teNode.getTePaths().isEmpty()) {
            for (ManagedTePath mngPath : teNode.getTePaths().values()) {
                switch (mngPath.getLsp().getPathStatus()) {
                    case Updated:
                        if (mngPath.getLsp().getComputedPath().getComputationStatus() != ComputationStatus.Completed) {
                            updateComputedPath(mngPath, false);
                        } else {
                            mngPath.updatePath(updateLsp);
                        }
                        break;
                    case Configured:
                        if (mngPath.getLsp().getComputedPath().getComputationStatus() != ComputationStatus.Completed) {
                            updateComputedPath(mngPath, true);
                        } else {
                            mngPath.addPath(addLsp);
                        }
                        break;
                    default:
                        break;
                }
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
        for (ManagedTePath mngPath : teNode.getTePaths().values()) {
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

    @Override
    public void verifyVertex(final Collection<ConnectedVertexTrigger> triggers, final ConnectedVertex current,
            final Vertex next) {
        for (ConnectedVertexTrigger trigger : triggers) {
            if (trigger.verifyVertex(current, next)) {
                updateComputedPath((ManagedTePath )trigger, false);
            }
        }
    }

    @Override
    public void verifyEdge(final Collection<ConnectedEdgeTrigger> triggers, final ConnectedEdge current,
            final Edge next) {
        for (ConnectedEdgeTrigger trigger : triggers) {
            if (trigger.verifyEdge(current, next)) {
                updateComputedPath((ManagedTePath )trigger, false);
            }
        }
    }
}
