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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements BgpDeployer, DataTreeChangeListener<Bgp>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);

    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final ExtendedBlueprintContainer container;
    private final BGPOpenConfigMappingService mappingService;
    private final ListenerRegistration<BgpDeployerImpl>  registration;
    private final DataBroker dataBroker;
    private final Map<InstanceIdentifier<Bgp>, RibImpl> ribs = new HashMap<>();

    public BgpDeployerImpl(final String networkInstanceName, final ExtendedBlueprintContainer container,
            final DataBroker dataBroker, final BGPOpenConfigMappingService mappingService) {
        this.container = container;
        this.dataBroker = dataBroker;
        this.mappingService = mappingService;
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
        this.registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, this.networkInstanceIId.child(Protocols.class)
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
                if (dataObjectModification.getDataType().equals(Global.class)) {
                    onGlobalChanged((DataObjectModification<Global>) dataObjectModification, rootIdentifier);
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
        this.ribs.values().forEach(rib -> rib.close());
        this.ribs.clear();
    }

    private static CheckedFuture<Void, TransactionCommitFailedException> initializeNetworkInstance(final DataBroker dataBroker,
            final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName()).setProtocols(new ProtocolsBuilder().build()).build());
        return wTx.submit();
    }

    private void onGlobalChanged(final DataObjectModification<Global> dataObjectModification,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        switch (dataObjectModification.getModificationType()) {
        case DELETE:
            onGlobalRemoved(rootIdentifier);
            break;
        case SUBTREE_MODIFIED:
            onGlobalModified(rootIdentifier, dataObjectModification.getDataAfter());
            break;
        case WRITE:
            onGlobalCreated(rootIdentifier, dataObjectModification.getDataAfter());
            break;
        default:
            break;
        }
    }

    private void onGlobalModified(final InstanceIdentifier<Bgp> rootIdentifier, final Global global) {
        //restart rib instance with a new configuration
        LOG.debug("Modifing RIB instance with configuration: {}", global);
        final RibImpl ribImpl = this.ribs.get(rootIdentifier);
        ribImpl.close();
        initiateRibInstance(rootIdentifier, global, ribImpl);
        LOG.debug("RIB instance modified {}", ribImpl);
    }

    private void onGlobalCreated(final InstanceIdentifier<Bgp> rootIdentifier, final Global global) {
        //create, start and register rib instance
        LOG.debug("Creating RIB instance with configuration: {}", global);
        final RibImpl ribImpl = (RibImpl) this.container.getComponentInstance(InstanceType.RIB.getBeanName());
        initiateRibInstance(rootIdentifier, global, ribImpl);
        this.ribs.put(rootIdentifier, ribImpl);
        LOG.debug("RIB instance created {}", ribImpl);
    }

    private void onGlobalRemoved(final InstanceIdentifier<Bgp> rootIdentifier) {
        //destroy rib instance
        LOG.debug("Removing RIB instance: {}", rootIdentifier);
        final RibImpl ribImpl = this.ribs.remove(rootIdentifier);
        ribImpl.close();
        LOG.debug("RIB instance removed {}", ribImpl);
    }

    private void registerRibInstance(final RibImpl ribImpl, final String ribInstanceName) {
        final Properties properties = new Properties();
        properties.setProperty(InstanceType.RIB.getBeanName(), ribInstanceName);
        final ServiceRegistration<RibImpl> serviceRegistration =
                this.container.registerService(InstanceType.RIB.getServices(), ribImpl, properties);
        ribImpl.setServiceRegistration(serviceRegistration);
    }

    private void initiateRibInstance(final InstanceIdentifier<Bgp> rootIdentifier, final Global global,
            final RibImpl ribImpl) {
        final String ribInstanceName = getRibInstanceName(rootIdentifier);
        ribImpl.start(global, ribInstanceName, this.mappingService);
        registerRibInstance(ribImpl, ribInstanceName);
    }

    @Override
    public <T extends DataObject> ListenableFuture<Void> writeConfiguration(final T data,
            final InstanceIdentifier<T> identifier) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, identifier, data);
        return wTx.submit();
    }

    @Override
    public <T extends DataObject> ListenableFuture<Void> removeConfiguration(
            final InstanceIdentifier<T> identifier) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, identifier);
        return wTx.submit();
    }

    @Override
    public BGPOpenConfigMappingService getMappingService() {
        return this.mappingService;
    }

}
