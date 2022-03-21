/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods required to manage the path
 * representation elements in the Data Store.
 *
 * @author Olivier Dugeon
 */

public final class PathManagerListener implements DataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerListener.class);
    private ListenerRegistration<PathManagerListener> listenerRegistration;

    private final PathManagerProvider pathManager;

    public PathManagerListener(final DataBroker dataBroker, KeyedInstanceIdentifier<Topology, TopologyKey> topology,
            final PathManagerProvider pathManager) {
        requireNonNull(dataBroker);
        requireNonNull(topology);
        this.pathManager = requireNonNull(pathManager);
        final InstanceIdentifier<Node> nodeTopology = topology.child(Node.class);
        this.listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, nodeTopology), this);
        LOG.info("Registered listener for Managed TE Path on Topology {}",
                topology.getKey().getTopologyId().getValue());
    }

    /**
     * Close this Listener.
     */
    @Override
    public void close() {
        if (this.listenerRegistration != null) {
            LOG.debug("Unregistered listener {} for Managed TE Path", this);
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    /**
     * Handle Configured LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param lspMod    List of Configured LSP modifications.
     */
    private void handleLspChange(NodeId nodeId, List<? extends DataObjectModification<? extends DataObject>> lspMod) {
        for (DataObjectModification<? extends DataObject> lsp : lspMod) {
            ConfiguredLsp cfgLsp;
            switch (lsp.getModificationType()) {
                case DELETE:
                    cfgLsp = (ConfiguredLsp) lsp.getDataBefore();
                    LOG.debug("Delete Managed TE Path: {}", cfgLsp.getName());
                    pathManager.deleteManagedTePath(nodeId, cfgLsp.key());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    cfgLsp = (ConfiguredLsp) lsp.getDataAfter();
                    LOG.debug("Update Managed TE Path {}", cfgLsp);
                    pathManager.createManagedTePath(nodeId, cfgLsp);
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * Parse Sub Tree modification. Given list has been filtered to get only Path Computation Client1 modifications.
     * This function first create, update or delete Managed TE Node that corresponds to the given NodeId. Then, it
     * filter the children to retain only the Configured LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param pccMod    List of PCEP Node Configuration modifications.
     */
    private void handlePccChange(NodeId nodeId, List<? extends DataObjectModification<? extends DataObject>> pccMod) {
        for (DataObjectModification<? extends DataObject> node : pccMod) {
            /* First, process PCC modification */
            switch (node.getModificationType()) {
                case DELETE:
                    LOG.debug("Delete Managed TE Node: {}", nodeId);
                    pathManager.deleteManagedTeNode(nodeId);
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    /* First look if the Managed TE Node belongs to this PCC was not already created */
                    final PcepNodeConfig pccNode = (PcepNodeConfig) node.getDataAfter();
                    if (!pathManager.checkManagedTeNode(nodeId)) {
                        LOG.info("Create new Managed Node {}", nodeId);
                        pathManager.createManagedTeNode(nodeId, pccNode);
                    } else {
                        /* Then, look to Configured LSP modification */
                        final List<DataObjectModification<? extends DataObject>> lspMod = node.getModifiedChildren()
                                .stream().filter(mod -> mod.getDataType().equals(ConfiguredLsp.class))
                                .collect(Collectors.toList());
                        if (!lspMod.isEmpty()) {
                            handleLspChange(nodeId, lspMod);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> root = change.getRootNode();

            final String nodeAddr = root.getModificationType() == DataObjectModification.ModificationType.DELETE
                    ? root.getDataBefore().getNodeId().getValue()
                    : root.getDataAfter().getNodeId().getValue();
            NodeId nodeId;
            if (nodeAddr.startsWith("pcc://")) {
                nodeId = new NodeId(nodeAddr);
            } else {
                nodeId = new NodeId("pcc://" + nodeAddr);
            }

            /* Look only to PcepNodeConfig.class modification */
            final List<DataObjectModification<? extends DataObject>> pccMod = root.getModifiedChildren().stream()
                    .filter(mod -> mod.getDataType().equals(PcepNodeConfig.class)).collect(Collectors.toList());
            if (!pccMod.isEmpty()) {
                handlePccChange(nodeId, pccMod);
            }
        }
    }
}

