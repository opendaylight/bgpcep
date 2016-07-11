/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getRibInstanceName;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
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
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements BgpDeployer, ClusteredDataTreeChangeListener<Bgp>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);

    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final ListenerRegistration<BgpDeployerImpl> registration;
    private final Map<InstanceIdentifier<Bgp>, RibImpl> ribs = new HashMap<>();
    private final ExtendedBlueprintContainer container;
    private final String networkInstanceName;

    public BgpDeployerImpl(final String networkInstanceName, final ExtendedBlueprintContainer container, final DataBroker dataBroker) {
        this.networkInstanceName = networkInstanceName;
        this.container = container;
        this.networkInstanceIId = InstanceIdentifier
                .create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        Futures.addCallback(initializeNetworkInstance(dataBroker, this.networkInstanceIId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Network Instance {} initialized successfully.", networkInstanceName);
            }
            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Network Instance {}.", networkInstanceName, t);
            }
        });
        this.registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                this.networkInstanceIId
                .child(Protocols.class)
                .child(Protocol.class)
                .augmentation(Protocol1.class)
                .child(Bgp.class)), this);
        LOG.info("BGP Deployer {} started.", networkInstanceName);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        for (final DataTreeModification<Bgp> dataTreeModification : changes) {
            final InstanceIdentifier<Bgp> rootIdentifier = dataTreeModification.getRootPath().getRootIdentifier();
            final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
            LOG.trace("BGP configuration has changed: {}", rootNode);
            for (final DataObjectModification<? extends DataObject> dataObjectModification : rootNode.getModifiedChildren()) {
                switch (dataObjectModification.getModificationType()) {
                    case DELETE:
                        onBgpRemoved(dataObjectModification, rootIdentifier);
                        break;
                    case SUBTREE_MODIFIED:
                        onBgpModified(dataObjectModification, rootIdentifier, rootNode.getDataAfter());
                    case WRITE:
                        onBgpCreated(dataObjectModification, rootIdentifier, rootNode.getDataAfter());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public InstanceIdentifier<NetworkInstance> getInstanceIdentifier() {
        return this.networkInstanceIId;
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
        //TODO close all peer instances
        this.ribs.values().forEach(ribImpl -> ribImpl.close());
        this.ribs.clear();
        LOG.info("BGP Deployer {} stopped.", this.networkInstanceName);
    }

    private void onBgpRemoved(final DataObjectModification<? extends DataObject> dataObjectModification, final InstanceIdentifier<Bgp> rootIdentifier) {
        LOG.trace("The BGP configuration subtree deleted: {}", dataObjectModification);
        if (dataObjectModification.getDataType().equals(Global.class)) {
            onGlobalRemoved(rootIdentifier);
        } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
            final Neighbors removed = (Neighbors)dataObjectModification.getDataBefore();
            if (removed.getNeighbor() != null) {
                for (final Neighbor neighbor : removed.getNeighbor()) {
                    //TODO destroy peer instance
                }
            }
        }
    }

    private void onBgpCreated(final DataObjectModification<? extends DataObject> dataObjectModification,
            final InstanceIdentifier<Bgp> rootIdentifier, final Bgp dataAfter) {
        LOG.trace("The BGP configuration subtree modified: {}", dataObjectModification);
        if (dataObjectModification.getDataType().equals(Global.class)) {
            final Global global = (Global) dataObjectModification.getDataAfter();
            onGlobalCreated(rootIdentifier, global);
        } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
            for (final DataObjectModification<? extends DataObject> modifiedNeighbors : dataObjectModification.getModifiedChildren()) {
                //TODO create peers instances
            }
        }
    }

    private void onBgpModified(final DataObjectModification<? extends DataObject> dataObjectModification, final InstanceIdentifier<Bgp> rootIdentifier,
            final Bgp bgp) {
        LOG.trace("The BGP configuration subtree modified: {}", dataObjectModification);
        if (dataObjectModification.getDataType().equals(Global.class)) {
            final Global global = (Global) dataObjectModification.getDataAfter();
            if (dataObjectModification.getModificationType().equals(ModificationType.WRITE)) {
                onGlobalCreated(rootIdentifier, global);
            } else {
                onGlobalModified(rootIdentifier, global);
            }
        } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
            for (final DataObjectModification<? extends DataObject> modifiedNeighbors : dataObjectModification.getModifiedChildren()) {
                switch (modifiedNeighbors.getModificationType()) {
                    case DELETE:
                        //TODO remove peer instance
                        break;
                    case SUBTREE_MODIFIED:
                        //TODO restart peer instance
                    case WRITE:
                        //TODO create peer instance
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void onGlobalModified(final InstanceIdentifier<Bgp> rootIdentifier, final Global global) {
        //restart rib instance with a new configuration
        final RibImpl ribImpl = this.ribs.get(rootIdentifier);
        ribImpl.close();
        final String ribInstanceName = getRibInstanceName(rootIdentifier);
        ribImpl.start(global, ribInstanceName);
        registerRibInstance(ribImpl, ribInstanceName);
    }

    private void onGlobalCreated(final InstanceIdentifier<Bgp> rootIdentifier, final Global global) {
        //create, start and register rib instance
        final RibImpl ribImpl = (RibImpl) this.container.getComponentInstance("ribImpl");
        final String ribInstanceName = getRibInstanceName(rootIdentifier);
        ribImpl.start(global, ribInstanceName);
        registerRibInstance(ribImpl, ribInstanceName);
        this.ribs.put(rootIdentifier, ribImpl);
    }

    private void registerRibInstance(final RibImpl ribImpl, final String ribInstanceName) {
        final Properties properties = new Properties();
        //FIXME to well-known constants
        properties.setProperty("rib-name", ribInstanceName);
        final ServiceRegistration<?> serviceRegistration = this.container.registerService(
                new String[] {RibReference.class.getName(), RIB.class.getName()}, ribImpl, properties);
        ribImpl.setServiceRegistration(serviceRegistration);
    }

    private void onGlobalRemoved(final InstanceIdentifier<Bgp> rootIdentifier) {
        //destroy rib instance
        this.ribs.remove(rootIdentifier).close();
    }

    private static CheckedFuture<Void, TransactionCommitFailedException> initializeNetworkInstance(final DataBroker dataBroker,
            final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName()).build());
        return wTx.submit();
    }

}
