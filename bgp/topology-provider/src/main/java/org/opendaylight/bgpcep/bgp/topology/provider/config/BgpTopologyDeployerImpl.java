/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.rib.spi.util.ClusterSingletonServiceRegistrationHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpTopologyDeployerImpl implements AutoCloseable, ClusteredDataTreeChangeListener<Topology>, BgpTopologyDeployer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpTopologyDeployerImpl.class);
    private static final InstanceIdentifier<Topology> TOPOLOGY_IID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class);

    @GuardedBy("this")
    private final Set<BgpTopologyProvider> topologyProviders = new HashSet<>();
    private final DataBroker dataBroker;
    private final ListenerRegistration<BgpTopologyDeployerImpl> registration;
    private final BundleContext context;
    private final ClusterSingletonServiceProvider singletonProvider;
    @GuardedBy("this")
    private boolean closed;

    public BgpTopologyDeployerImpl(final BundleContext context, final DataBroker dataBroker, final ClusterSingletonServiceProvider singletonProvider) {
        this.context = requireNonNull(context);
        this.dataBroker = requireNonNull(dataBroker);
        this.singletonProvider = requireNonNull(singletonProvider);
        this.registration =
            this.dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(NetworkTopology.class).child(Topology.class)), this);
        LOG.info("BGP topology deployer started.");
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        if (this.closed) {
            LOG.trace("BGP Topology Provider Deployer was already closed, skipping changes.");
            return;
        }
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
                filterTopologyBuilders(dataAfter).forEach(provider -> provider.onTopologyBuilderCreated(dataAfter, null));
                break;
            case WRITE:
                filterTopologyBuilders(dataAfter).forEach(provider -> provider.onTopologyBuilderCreated(dataAfter, null));
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
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public AbstractRegistration registerService(final TopologyReferenceSingletonService topologyProviderService) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put("topology-id", topologyProviderService.getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
        final ServiceRegistration<?> registerService = this.context.registerService(new String[]{TopologyReference.class.getName()}, topologyProviderService, properties);
        final ClusterSingletonServiceRegistration registerClusterSingletonService = registerSingletonService(topologyProviderService);
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

    @Override
    public void createInstance(final Topology topology) {
        final Function<Topology, Void> writeFunction = topology1 -> {
            final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
            final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIIdKeyed =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topology1.getKey());
            wTx.put(LogicalDatastoreType.CONFIGURATION, topologyIIdKeyed, topology1, true);
            wTx.submit();
            return null;
        };

        filterTopologyBuilders(topology).forEach(provider -> provider.onTopologyBuilderCreated(topology, writeFunction));
    }

    @Override
    public void removeInstance(final Topology topology) {
        filterTopologyBuilders(topology).forEach(provider -> provider.onTopologyBuilderRemoved(topology));
    }

    @Override
    public InstanceIdentifier<Topology> getInstanceIdentifier() {
        return TOPOLOGY_IID;
    }

    @Override
    public synchronized void close() {
        this.registration.close();
        LOG.info("BGP topology deployer stopped.");
        this.closed = true;
    }

    private Iterable<BgpTopologyProvider> filterTopologyBuilders(final Topology topology) {
        return this.topologyProviders.stream().filter(input -> input.topologyTypeFilter(topology))
            .collect(Collectors.toList());
    }

    private ClusterSingletonServiceRegistration registerSingletonService(
        final ClusterSingletonService clusterSingletonService) {
        return ClusterSingletonServiceRegistrationHelper
            .registerSingletonService(this.singletonProvider, clusterSingletonService);
    }

}
