/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployer implements ClusteredDataTreeChangeListener<Bgp>, AutoCloseable {

    //TODO make configurable
    private static final String GLOBAL_BGP = "global-bgp";

    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployer.class);

    private final BgpDeploymentManager bgpDeploymentManager;
    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final ListenerRegistration<BgpDeployer> registration;

    public BgpDeployer(final BgpDeploymentManager bgpDeploymentManager) {
        this.bgpDeploymentManager = bgpDeploymentManager;
        this.networkInstanceIId = InstanceIdentifier
                .create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(GLOBAL_BGP));
        Futures.addCallback(initializeNetworkInstance(this.bgpDeploymentManager.getDataBroker(), this.networkInstanceIId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Network Instance {} initialized successfully.", GLOBAL_BGP);
            }
            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Network Instance {}.", GLOBAL_BGP, t);
            }
        });
        this.registration = this.bgpDeploymentManager.getDataBroker().registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                this.networkInstanceIId
                    .child(Protocols.class)
                    .child(Protocol.class)
                    .augmentation(Protocol1.class)
                    .child(Bgp.class)), this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        for (final DataTreeModification<Bgp> dataTreeModification : changes) {
            final InstanceIdentifier<Bgp> rootIdentifier = dataTreeModification.getRootPath().getRootIdentifier();
            final InstanceIdentifier<Protocol> protocolIId = rootIdentifier.firstIdentifierOf(Protocol.class);
            final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
            for (final DataObjectModification<? extends DataObject> dataObjectModification : rootNode.getModifiedChildren()) {
                switch (dataObjectModification.getModificationType()) {
                case DELETE:
                    onDeletedBgp(dataObjectModification, protocolIId);
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    onModifiedBgp(dataObjectModification, protocolIId, rootNode.getDataBefore());
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
        this.bgpDeploymentManager.closeInstances(this.networkInstanceIId);
    }

    private void onDeletedBgp(final DataObjectModification<? extends DataObject> dataObjectModification, final InstanceIdentifier<Protocol> protocolIId) {
        if (dataObjectModification.getDataType().equals(Global.class)) {
            this.bgpDeploymentManager.onGlobalRemoved(protocolIId);
        } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
            final Neighbors removed = (Neighbors)dataObjectModification.getDataBefore();
            for (final Neighbor neighbor : removed.getNeighbor()) {
                this.bgpDeploymentManager.onNeighborRemoved(protocolIId, neighbor);
            }
        }
    }

    private void onModifiedBgp(final DataObjectModification<? extends DataObject> dataObjectModification, final InstanceIdentifier<Protocol> protocolIId,
            final Bgp bgp) {
        if (dataObjectModification.getDataType().equals(Global.class)) {
            final Global global = (Global) dataObjectModification.getDataAfter();
            if (dataObjectModification.getModificationType().equals(ModificationType.WRITE)) {
                this.bgpDeploymentManager.onGlobalCreated(protocolIId, global);
            } else {
                this.bgpDeploymentManager.onGlobalModified(protocolIId, bgp);
            }
        } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
            for (final DataObjectModification<? extends DataObject> modifiedNeighbors : dataObjectModification.getModifiedChildren()) {
                switch (modifiedNeighbors.getModificationType()) {
                case DELETE:
                    final Neighbor removedNeighbor = (Neighbor) modifiedNeighbors.getDataBefore();
                    this.bgpDeploymentManager.onNeighborRemoved(protocolIId, removedNeighbor);
                    break;
                case SUBTREE_MODIFIED:
                    final Neighbor updatedNeighbor = (Neighbor) modifiedNeighbors.getDataAfter();
                    this.bgpDeploymentManager.onNeighborModified(protocolIId, updatedNeighbor);
                case WRITE:
                    final Neighbor createNeighbor = (Neighbor) modifiedNeighbors.getDataAfter();
                    this.bgpDeploymentManager.onNeighborCreated(protocolIId, createNeighbor);
                    break;
                default:
                    break;
                }
            }
        }
    }

    private CheckedFuture<Void, TransactionCommitFailedException> initializeNetworkInstance(final DataBroker dataBroker,
            final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName()).build());
        return wTx.submit();
    }

}
