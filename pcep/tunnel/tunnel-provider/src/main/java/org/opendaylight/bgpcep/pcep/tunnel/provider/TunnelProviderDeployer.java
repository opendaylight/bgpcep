/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.config.rev181109.PcepTunnelTopologyConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TunnelProviderDeployer implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelProviderDeployer.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private final TunnelProviderDependencies dependencies;
    private final InstanceIdentifier<Topology> networTopology;
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTunnelClusterSingletonService> pcepTunnelServices = new HashMap<>();
    @GuardedBy("this")
    private ListenerRegistration<TunnelProviderDeployer> listenerRegistration;

    public TunnelProviderDeployer(
            final DataBroker dataBroker,
            final RpcProviderService rpcProviderRegistry,
            final RpcConsumerRegistry rpcConsumerRegistry,
            final BundleContext bundleContext,
            final ClusterSingletonServiceProvider cssp
    ) {
        LOG.info("Creating Tunnel Provider Deployer");
        this.dependencies = new TunnelProviderDependencies(dataBroker, cssp, rpcProviderRegistry, rpcConsumerRegistry,
                bundleContext);
        this.networTopology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).build();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void closeTopology(final PCEPTunnelClusterSingletonService topology, final TopologyId topologyId) {
        if (topology != null) {
            try {
                topology.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (final Exception e) {
                LOG.error("Topology {} instance failed to close service instance", topologyId, e);
            }
            topology.close();
        }
    }

    public synchronized void init() {
        LOG.info("Instantiate tunnel topology deployer");
        this.listenerRegistration = this.dependencies.getDataBroker().registerDataTreeChangeListener(
                DataTreeIdentifier.create(CONFIGURATION, this.networTopology), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        final List<DataObjectModification<Topology>> topoChanges = changes.stream()
                .map(DataTreeModification::getRootNode)
                .collect(Collectors.toList());

        topoChanges.stream().iterator().forEachRemaining(this::handleTopologyChange);
    }

    private synchronized void handleTopologyChange(final DataObjectModification<Topology> topo) {
        switch (topo.getModificationType()) {
            case SUBTREE_MODIFIED:
                updateTunnelTopologyProvider(topo.getDataAfter());
                break;
            case WRITE:
                createTunnelTopologyProvider(topo.getDataAfter());
                break;
            case DELETE:
                removeTunnelTopologyProvider(topo.getDataBefore());
                break;
            default:
        }
    }

    private static boolean filterPcepTopologies(final TopologyTypes topologyTypes) {
        if (topologyTypes == null) {
            return false;
        }
        final TopologyTypes1 aug = topologyTypes.augmentation(TopologyTypes1.class);
        return aug != null && aug.getTopologyTunnelPcep() != null;
    }

    private synchronized void createTunnelTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        if (this.pcepTunnelServices.containsKey(topology.getTopologyId())) {
            LOG.warn("Tunnel Topology {} already exist. New instance won't be created", topologyId);
            return;
        }
        LOG.debug("Create Tunnel Topology {}", topologyId);

        final PcepTunnelTopologyConfig config = topology.augmentation(PcepTunnelTopologyConfig.class);
        final String pcepTopoID = StringUtils
                .substringBetween(config.getPcepTopologyReference().getValue(), "=\"", "\"");
        final InstanceIdentifier<Topology> pcepTopoRef = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(pcepTopoID))).build();


        final PCEPTunnelClusterSingletonService tunnelTopoCss =
                new PCEPTunnelClusterSingletonService(this.dependencies, pcepTopoRef, topologyId);
        this.pcepTunnelServices.put(topology.getTopologyId(), tunnelTopoCss);
    }

    private synchronized void updateTunnelTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        final PCEPTunnelClusterSingletonService previous = this.pcepTunnelServices.remove(topology.getTopologyId());
        closeTopology(previous, topologyId);
        createTunnelTopologyProvider(topology);
    }

    private synchronized void removeTunnelTopologyProvider(final Topology topo) {
        if (!filterPcepTopologies(topo.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topo.getTopologyId();
        final PCEPTunnelClusterSingletonService topology = this.pcepTunnelServices.remove(topologyId);
        closeTopology(topology, topologyId);
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing tunnel topology deployer");
        if (this.listenerRegistration != null) {
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
        this.pcepTunnelServices.values().iterator().forEachRemaining(PCEPTunnelClusterSingletonService::close);
        this.pcepTunnelServices.clear();
    }
}
