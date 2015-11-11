/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.moduleconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPOpenConfigListener implements DataTreeChangeListener<Bgp>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPOpenConfigListener.class);

    private final BGPConfigModuleProvider configModuleWriter;
    private final ListenerRegistration<BGPOpenConfigListener> registerDataTreeChangeListener;
    private final BGPRibImplProvider ribImplProvider;
    private final BGPPeerProvider peerProvider;
    private final BGPAppPeerProvider appPeerProvider;
    private final Callable<MountPoint> callable;

    public BGPOpenConfigListener(final DataBroker dataBroker, final Callable<MountPoint> callable, final BGPConfigStateStore configStateHolders) {
        this.callable = Preconditions.checkNotNull(callable);
        this.configModuleWriter = new BGPConfigModuleProvider();
        this.ribImplProvider = new BGPRibImplProvider(configStateHolders, configModuleWriter);
        this.peerProvider = new BGPPeerProvider(configStateHolders, configModuleWriter);
        this.appPeerProvider = new BGPAppPeerProvider(configStateHolders, configModuleWriter);
        this.registerDataTreeChangeListener = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, OpenConfigUtil.BGP_IID), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        try {
            final Optional<DataBroker> databroker = callable.call().getService(DataBroker.class);
            if (databroker.isPresent()) {
                for (final DataTreeModification<Bgp> dataTreeModification : changes) {
                    final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
                    for (final DataObjectModification<? extends DataObject> dataObjectModification : rootNode.getModifiedChildren()) {
                        final DataBroker db = databroker.get();
                        switch (dataObjectModification.getModificationType()) {
                        case DELETE:
                            onOpenConfigRemoved(dataObjectModification.getDataBefore(), db);
                            break;
                        case SUBTREE_MODIFIED:
                        case WRITE:
                            onOpenConfigModified(dataObjectModification, db);
                            break;
                        default:
                            throw new IllegalArgumentException("Unhandled modification type " + rootNode.getModificationType());
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to obtain MountPoint.", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        registerDataTreeChangeListener.close();
    }

    private void onOpenConfigRemoved(final DataObject removedData, final DataBroker dataBroker) {
        if (removedData instanceof Global) {
            ribImplProvider.onGlobalRemoved((Global) removedData, dataBroker);
        } else if (removedData instanceof Neighbors) {
            final Neighbors neighbors = (Neighbors) removedData;
            for (final Neighbor neighbor : neighbors.getNeighbor()) {
                removeNeighbor(neighbor, dataBroker);
            }
        } else {
            LOG.info("Skipping unhandled removed data: {}", removedData);
        }
    }

    private void onOpenConfigModified(final DataObjectModification<? extends DataObject> dataObjectModification, final DataBroker dataBroker) {
        final DataObject modifiedData = dataObjectModification.getDataAfter();
        if (modifiedData instanceof Global) {
            ribImplProvider.onGlobalModified((Global) modifiedData, dataBroker);
        } else if (modifiedData instanceof Neighbors) {
            for (final DataObjectModification<? extends DataObject> childModification : dataObjectModification.getModifiedChildren()) {
                switch (childModification.getModificationType()) {
                case DELETE:
                    final Neighbor before = (Neighbor) childModification.getDataBefore();
                    removeNeighbor(before, dataBroker);
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    final Neighbor after = (Neighbor) childModification.getDataAfter();
                    modifyNeighbor(after, dataBroker);
                    break;
                default:
                    break;
                }
            }
        } else {
            LOG.info("Skipping unhandled modified data: {}", modifiedData);
        }
    }

    private void removeNeighbor(final Neighbor neighbor, final DataBroker dataBroker) {
        if (isAppNeighbor(neighbor)) {
            appPeerProvider.onNeighborRemoved(neighbor, dataBroker);
        } else {
            peerProvider.onNeighborRemoved(neighbor, dataBroker);
        }
    }

    private void modifyNeighbor(final Neighbor neighbor, final DataBroker dataBroker) {
        if (isAppNeighbor(neighbor)) {
            appPeerProvider.onNeighborModified(neighbor, dataBroker);
        } else {
            peerProvider.onNeighborModified(neighbor, dataBroker);
        }
    }

    private static boolean isAppNeighbor(final Neighbor neighbor) {
        final Config1 config1 = neighbor.getConfig().getAugmentation(Config1.class);
        if (config1 != null) {
            return config1.getPeerGroup() != null && config1.getPeerGroup().equals(OpenConfigUtil.APPLICATION_PEER_GROUP_NAME);
        }
        return false;
    }
}
