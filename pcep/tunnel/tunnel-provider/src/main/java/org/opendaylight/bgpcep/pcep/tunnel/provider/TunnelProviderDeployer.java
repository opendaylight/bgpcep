/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.config.rev181109.PcepTunnelTopologyConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { })
public final class TunnelProviderDeployer implements DataTreeChangeListener<Topology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelProviderDeployer.class);
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private final TunnelProviderDependencies dependencies;
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTunnelClusterSingletonService> pcepTunnelServices = new HashMap<>();
    private final Registration reg;

    @Activate
    public TunnelProviderDeployer(@Reference final DataBroker dataBroker,
            @Reference final RpcProviderService rpcProviderRegistry,
            @Reference final RpcService rpcService,
            @Reference final ClusterSingletonServiceProvider cssp,
            // FIXME: do not reference BundleContext in an alternative constructor
            final BundleContext bundleContext) {
        dependencies = new TunnelProviderDependencies(dataBroker, cssp, rpcProviderRegistry, rpcService,
                bundleContext);
        reg = dataBroker.registerTreeChangeListener(CONFIGURATION,
                DataObjectReference.builder(NetworkTopology.class).child(Topology.class).build(), this);
        LOG.info("Tunnel Provider Deployer created");
    }

    @Deactivate
    @Override
    public synchronized void close() {
        reg.close();
        pcepTunnelServices.values().iterator().forEachRemaining(PCEPTunnelClusterSingletonService::close);
        pcepTunnelServices.clear();
        LOG.info("Tunnel Provider Deployer closed");
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

    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeModification<Topology>> changes) {
        changes.stream().map(DataTreeModification::getRootNode).forEach(topo -> {
            switch (topo.modificationType()) {
                case SUBTREE_MODIFIED:
                    updateTunnelTopologyProvider(topo.dataAfter());
                    break;
                case WRITE:
                    createTunnelTopologyProvider(topo.dataAfter());
                    break;
                case DELETE:
                    removeTunnelTopologyProvider(topo.dataBefore());
                    break;
                default:
            }
        });
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
        final var topologyId = topology.getTopologyId();
        if (pcepTunnelServices.containsKey(topology.getTopologyId())) {
            LOG.warn("Tunnel Topology {} already exist. New instance won't be created", topologyId);
            return;
        }
        LOG.debug("Create Tunnel Topology {}", topologyId);

        final var config = topology.augmentation(PcepTunnelTopologyConfig.class);
        final var pcepTopoID = StringUtils.substringBetween(config.getPcepTopologyReference().getValue(), "=\"", "\"");
        final var pcepTopoRef = DataObjectIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(pcepTopoID)))
                .build();

        final var tunnelTopoCss = new PCEPTunnelClusterSingletonService(dependencies, pcepTopoRef, topologyId);
        pcepTunnelServices.put(topology.getTopologyId(), tunnelTopoCss);
    }

    private synchronized void updateTunnelTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        final PCEPTunnelClusterSingletonService previous = pcepTunnelServices.remove(topology.getTopologyId());
        closeTopology(previous, topologyId);
        createTunnelTopologyProvider(topology);
    }

    private synchronized void removeTunnelTopologyProvider(final Topology topo) {
        if (!filterPcepTopologies(topo.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topo.getTopologyId();
        final PCEPTunnelClusterSingletonService topology = pcepTunnelServices.remove(topologyId);
        closeTopology(topology, topologyId);
    }
}
