/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpTopologyDeployerImpl implements AutoCloseable, ClusteredDataTreeChangeListener<Topology>, BgpTopologyDeployer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpTopologyDeployerImpl.class);

    @GuardedBy("this")
    private final Set<BgpTopologyProvider> topologyProviders = new HashSet<>();
    private final DataBroker dataBroker;
    private final ListenerRegistration<BgpTopologyDeployerImpl> registration;
    private final ExtendedBlueprintContainer container;
    private final ClusterSingletonServiceProvider singletonProvider;

    public BgpTopologyDeployerImpl(final ExtendedBlueprintContainer container, final DataBroker dataBroker, final ClusterSingletonServiceProvider singletonProvider) {
        this.container = Preconditions.checkNotNull(container);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.singletonProvider = Preconditions.checkNotNull(singletonProvider);
        this.registration =
                this.dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<Topology>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class).child(Topology.class)), this);
        LOG.info("BGP topology deployer started.");
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        for (final DataTreeModification<Topology> change : changes) {
            final DataObjectModification<Topology> rootNode = change.getRootNode();
            final Topology dataBefore = rootNode.getDataBefore();
            final Topology dataAfter = rootNode.getDataAfter();
            LOG.trace("BGP topology deployer configuration changed: modification type: [{}], data before:[{}], data after: [{}]", rootNode.getModificationType(), dataBefore, dataAfter);
            switch (rootNode.getModificationType()) {
            case DELETE:
                filterTopologyBuilders(dataBefore).forEach(provider -> provider.onTopologyBuilderRemoved(dataBefore));
                break;
            case SUBTREE_MODIFIED:
                filterTopologyBuilders(dataBefore).forEach(provider -> provider.onTopologyBuilderRemoved(dataBefore));
                filterTopologyBuilders(dataAfter).forEach(provider -> provider.onTopologyBuilderCreated(dataAfter));
                break;
            case WRITE:
                filterTopologyBuilders(dataAfter).forEach(provider -> provider.onTopologyBuilderCreated(dataAfter));
                break;
            default:
                break;
            }
        }
    }

    @Override
    public synchronized AbstractRegistration registerTopologyProvider(final BgpTopologyProvider topologyBuilder) {
        this.topologyProviders.add(topologyBuilder);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (BgpTopologyDeployerImpl.this) {
                    BgpTopologyDeployerImpl.this.topologyProviders.remove(topologyBuilder);
                }
            }
        };
    }

    @Override
    public void close() throws Exception {
        this.registration.close();
        LOG.info("BGP topology deployer stopped.");
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    private synchronized Iterable<BgpTopologyProvider> filterTopologyBuilders(final Topology topology) {
        return Iterables.filter(this.topologyProviders, input -> input.topologyTypeFilter(topology));
    }

    @Override
    public AbstractRegistration registerService(final TopologyReferenceSingletonService topologyProviderService) {
        final Properties properties = new Properties();
        properties.setProperty("topology-id", topologyProviderService.getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
        final ServiceRegistration<?> registerService = this.container.registerService(new String[] {TopologyReference.class.getName()}, topologyProviderService, properties);
        final ClusterSingletonServiceRegistration registerClusterSingletonService = this.singletonProvider.registerClusterSingletonService(topologyProviderService);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                try {
                    registerClusterSingletonService.close();
                } catch (final Exception e) {
                    LOG.warn("Failed to close ClusterSingletonServiceRegistration {} for TopologyBuilder {}",
                            registerClusterSingletonService, topologyProviderService.getInstanceIdentifier(), e);
                }
                registerService.unregister();
            }
        };
    }

}
