/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.concepts.Registration;
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

    private final PathManagerProvider pathManager;

    private Registration listenerRegistration;

    public PathManagerListener(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topology,
            final PathManagerProvider pathManager) {
        this.pathManager = requireNonNull(pathManager);
        listenerRegistration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION,
            topology.toBuilder().toReferenceBuilder().child(Node.class).build(), this);
        LOG.info("Registered listener for Managed TE Path on Topology {}", topology.key().getTopologyId().getValue());
    }

    /**
     * Close this Listener.
     */
    @Override
    public void close() {
        if (listenerRegistration != null) {
            LOG.debug("Unregistered listener {} for Managed TE Path", this);
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }

    /**
     * Handle Configured LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param lspMod    List of Configured LSP modifications.
     */
    private void handleLspChange(final NodeId nodeId, final List<? extends DataObjectModification<?>> lspMod) {
        for (DataObjectModification<? extends DataObject> lsp : lspMod) {
            ConfiguredLsp cfgLsp;
            switch (lsp.modificationType()) {
                case DELETE:
                    cfgLsp = (ConfiguredLsp) lsp.dataBefore();
                    LOG.debug("Delete Managed TE Path: {}", cfgLsp.getName());
                    pathManager.deleteManagedTePath(nodeId, cfgLsp.key());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    cfgLsp = (ConfiguredLsp) lsp.dataAfter();
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
    private void handlePccChange(final NodeId nodeId, final List<? extends DataObjectModification<?>> pccMod) {
        for (var node : pccMod) {
            /* First, process PCC modification */
            switch (node.modificationType()) {
                case DELETE:
                    LOG.debug("Delete Managed TE Node: {}", nodeId);
                    pathManager.deleteManagedTeNode(nodeId);
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    /* First look if the Managed TE Node belongs to this PCC was not already created */
                    final var pccNode = (PcepNodeConfig) node.dataAfter();
                    if (!pathManager.checkManagedTeNode(nodeId)) {
                        LOG.info("Create new Managed Node {}", nodeId);
                        pathManager.createManagedTeNode(nodeId, pccNode);
                    } else {
                        /* Then, look to Configured LSP modification */
                        final var lspMod = node.modifiedChildren()
                                .stream().filter(mod -> mod.dataType().equals(ConfiguredLsp.class))
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
    public void onDataTreeChanged(final List<DataTreeModification<Node>> changes) {
        for (var change : changes) {
            final var root = change.getRootNode();

            final String nodeAddr = root.modificationType() == DataObjectModification.ModificationType.DELETE
                    ? root.dataBefore().getNodeId().getValue()
                    : root.dataAfter().getNodeId().getValue();
            NodeId nodeId;
            if (nodeAddr.startsWith("pcc://")) {
                nodeId = new NodeId(nodeAddr);
            } else {
                nodeId = new NodeId("pcc://" + nodeAddr);
            }

            /* Look only to PcepNodeConfig.class modification */
            final var pccMod = root.modifiedChildren().stream()
                    .filter(mod -> mod.dataType().equals(PcepNodeConfig.class))
                    .collect(Collectors.toList());
            if (!pccMod.isEmpty()) {
                handlePccChange(nodeId, pccMod);
            }
        }
    }
}
