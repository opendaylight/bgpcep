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
import java.util.Properties;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements BgpDeployer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);

    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final ExtendedBlueprintContainer container;
    private final BGPOpenConfigMappingService mappingService;
    private final DataBroker dataBroker;

    public BgpDeployerImpl(final String networkInstanceName, final ExtendedBlueprintContainer container, final DataBroker dataBroker,
            final BGPOpenConfigMappingService mappingService) {
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
        LOG.info("BGP Deployer {} started.", networkInstanceName);
    }

    @Override
    public InstanceIdentifier<NetworkInstance> getInstanceIdentifier() {
        return this.networkInstanceIId;
    }

    private static CheckedFuture<Void, TransactionCommitFailedException> initializeNetworkInstance(final DataBroker dataBroker,
            final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName()).setProtocols(new ProtocolsBuilder().build()).build());
        return wTx.submit();
    }

    @Override
    public <T extends DataObject> ListenerRegistration<?> registerDataTreeChangeListener(
            final DataTreeChangeListener<T> listener, final InstanceIdentifier<T> path) {
        return this.dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<T>(LogicalDatastoreType.CONFIGURATION, path), listener);
    }

    @Override
    public BGPOpenConfigMappingService getMappingService() {
        return this.mappingService;
    }

    @Override
    public Object getComponentInstance(final InstanceType instanceType) {
        return this.container.getComponentInstance(instanceType.getBeanName());
    }

    @Override
    public <T> ServiceRegistration<T> registerService(final InstanceType instanceType, final T service,
            final String instanceName) {
        final Properties properties = new Properties();
        properties.setProperty(instanceType.getBeanName(), instanceName);
        return this.container.registerService(instanceType.getServices(), service, properties);
    }

}
