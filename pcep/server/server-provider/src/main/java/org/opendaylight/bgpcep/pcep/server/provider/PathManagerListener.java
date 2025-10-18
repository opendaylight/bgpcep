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
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.WithDataAfter;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PcepNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
public final class PathManagerListener implements DataTreeChangeListener<PcepNodeConfig>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PathManagerListener.class);

    private final PathManagerProvider pathManager;

    private Registration listenerRegistration;

    public PathManagerListener(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topology,
            final PathManagerProvider pathManager) {
        this.pathManager = requireNonNull(pathManager);
        listenerRegistration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION,
            topology.toBuilder().toReferenceBuilder().child(Node.class).augmentation(PcepNodeConfig.class).build(),
            this);
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

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<PcepNodeConfig>> changes) {
        for (var change : changes) {
            final var nodeAddr = change.path().getFirstKeyOf(Node.class).getNodeId().getValue();
            final var nodeId = new NodeId(nodeAddr.startsWith("pcc://") ? nodeAddr : "pcc://" + nodeAddr);

            switch (change.getRootNode()) {
                case DataObjectDeleted<?> deleted -> {
                    LOG.debug("Delete Managed TE Node: {}", nodeId);
                    pathManager.deleteManagedTeNode(nodeId);
                }
                case WithDataAfter<PcepNodeConfig> present -> {
                    /* First look if the Managed TE Node belongs to this PCC was not already created */
                    final var pccNode = present.dataAfter();
                    if (!pathManager.checkManagedTeNode(nodeId)) {
                        LOG.info("Create new Managed Node {}", nodeId);
                        pathManager.createManagedTeNode(nodeId, pccNode);
                    } else {
                        /* Then, look to Configured LSP modification */
                        handleLspChange(nodeId, present.getModifiedChildren(ConfiguredLsp.class));
                    }
                }
            }
        }
    }

    /**
     * Handle Configured LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param changes    List of Configured LSP modifications.
     */
    private void handleLspChange(final NodeId nodeId, final Collection<DataObjectModification<ConfiguredLsp>> changes) {
        for (var change : changes) {
            switch (change) {
                case DataObjectDeleted<ConfiguredLsp> deleted -> {
                    final var key = deleted.coerceKeyStep(ConfiguredLsp.class).key();
                    LOG.debug("Delete Managed TE Path: {}", key.getName());
                    pathManager.deleteManagedTePath(nodeId, key);
                }
                case WithDataAfter<ConfiguredLsp> present -> {
                    final var cfgLsp = present.dataAfter();
                    LOG.debug("Update Managed TE Path {}", cfgLsp);
                    pathManager.createManagedTePath(nodeId, cfgLsp);
                }
            }
        }
    }
}
