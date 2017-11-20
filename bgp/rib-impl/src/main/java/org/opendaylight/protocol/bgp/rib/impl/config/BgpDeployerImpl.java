/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
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
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpDeployerImpl implements ClusteredDataTreeChangeListener<Bgp>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BgpDeployerImpl.class);
    private final InstanceIdentifier<NetworkInstance> networkInstanceIId;
    private final BlueprintContainer container;
    private final BundleContext bundleContext;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final ClusterSingletonServiceProvider provider;
    private ListenerRegistration<BgpDeployerImpl> registration;
    @GuardedBy("this")
    private final Map<InstanceIdentifier<Bgp>, BGPClusterSingletonService> bgpCss = new HashMap<>();
    private final DataBroker dataBroker;
    private final String networkInstanceName;
    @GuardedBy("this")
    private boolean closed;

    public BgpDeployerImpl(final String networkInstanceName, final ClusterSingletonServiceProvider provider,
            final BlueprintContainer container,
            final BundleContext bundleContext, final DataBroker dataBroker,
            final BGPTableTypeRegistryConsumer mappingService) {
        this.dataBroker = requireNonNull(dataBroker);
        this.provider = requireNonNull(provider);
        this.networkInstanceName = requireNonNull(networkInstanceName);
        this.container = requireNonNull(container);
        this.bundleContext = requireNonNull(bundleContext);
        this.tableTypeRegistry = requireNonNull(mappingService);
        this.networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
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
        }, MoreExecutors.directExecutor());
    }

    public void init() {
        this.registration = this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        this.networkInstanceIId.child(Protocols.class)
                                .child(Protocol.class).augmentation(Protocol1.class).child(Bgp.class)), this);
        LOG.info("BGP Deployer {} started.", this.networkInstanceName);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Bgp>> changes) {
        if (this.closed) {
            LOG.trace("BGP Deployer was already closed, skipping changes.");
            return;
        }
        for (final DataTreeModification<Bgp> dataTreeModification : changes) {
            final InstanceIdentifier<Bgp> rootIdentifier = dataTreeModification.getRootPath().getRootIdentifier();
            final DataObjectModification<Bgp> rootNode = dataTreeModification.getRootNode();
            LOG.trace("BGP configuration has changed: {}", rootNode);
            for (final DataObjectModification<? extends DataObject> dataObjectModification :
                    rootNode.getModifiedChildren()) {
                if (dataObjectModification.getDataType().equals(Global.class)) {
                    onGlobalChanged((DataObjectModification<Global>) dataObjectModification, rootIdentifier);
                } else if (dataObjectModification.getDataType().equals(Neighbors.class)) {
                    onNeighborsChanged((DataObjectModification<Neighbors>) dataObjectModification, rootIdentifier);
                }
            }
        }
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.info("Closing BGP Deployer.");
        if (this.registration != null) {
            this.registration.close();
        }
        this.closed = true;

        this.bgpCss.values().iterator().forEachRemaining(service -> {
            try {
                service.close();
            } catch (Exception e) {
                LOG.warn("Failed to close BGP Cluster Singleton Service.");
            }
        });

    }

    private static ListenableFuture<Void> initializeNetworkInstance(
            final DataBroker dataBroker, final InstanceIdentifier<NetworkInstance> networkInstance) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, networkInstance,
                new NetworkInstanceBuilder().setName(networkInstance.firstKeyOf(NetworkInstance.class).getName())
                        .setProtocols(new ProtocolsBuilder().build()).build());
        return wTx.submit();
    }

    @VisibleForTesting
    synchronized void onGlobalChanged(final DataObjectModification<Global> dataObjectModification,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        BGPClusterSingletonService old = this.bgpCss.get(rootIdentifier);
        if (old == null) {
            old = new BGPClusterSingletonService(this.provider, this.tableTypeRegistry, this.container,
                    this.bundleContext, rootIdentifier);
            this.bgpCss.put(rootIdentifier, old);
        }
        old.onGlobalChanged(dataObjectModification);
    }

    @VisibleForTesting
    synchronized void onNeighborsChanged(final DataObjectModification<Neighbors> dataObjectModification,
            final InstanceIdentifier<Bgp> rootIdentifier) {
        BGPClusterSingletonService old = this.bgpCss.get(rootIdentifier);
        if (old == null) {
            old = new BGPClusterSingletonService(this.provider, this.tableTypeRegistry, this.container,
                    this.bundleContext, rootIdentifier);
            this.bgpCss.put(rootIdentifier, old);
        }
        old.onNeighborsChanged(dataObjectModification);
    }


    @VisibleForTesting
    BGPTableTypeRegistryConsumer getTableTypeRegistry() {
        return this.tableTypeRegistry;
    }
}
