/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTopologyDeployerImpl implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private final InstructionSchedulerFactory instructionSchedulerFactory;
    private final ClusterSingletonServiceProvider singletonService;
    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderRegistry;
    private final TopologySessionStatsRegistry stateRegistry;
    private final PceServerProvider pceServerProvider;
    private final BundleContext bundleContext;

    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderBean> pcepTopologyServices = new HashMap<>();
    @GuardedBy("this")
    private ListenerRegistration<PCEPTopologyDeployerImpl> listenerRegistration;

    public PCEPTopologyDeployerImpl(final DataBroker dataBroker, final ClusterSingletonServiceProvider singletonService,
            final RpcProviderService rpcProviderRegistry, final PCEPDispatcher pcepDispatcher,
            final InstructionSchedulerFactory instructionSchedulerFactory,
            final TopologySessionStatsRegistry stateRegistry, final PceServerProvider pceServerProvider,
            // FIXME: we should not be needing this OSGi dependency
            final BundleContext bundleContext) {
        this.dataBroker = requireNonNull(dataBroker);
        this.singletonService = requireNonNull(singletonService);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.pcepDispatcher = requireNonNull(pcepDispatcher);
        this.instructionSchedulerFactory = requireNonNull(instructionSchedulerFactory);
        this.stateRegistry = requireNonNull(stateRegistry);
        this.pceServerProvider = requireNonNull(pceServerProvider);
        this.bundleContext = requireNonNull(bundleContext);

        LOG.info("PCEP Topology Deployer initialized");
        listenerRegistration = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).build()), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        for (var change : changes) {
            final var topo = change.getRootNode();
            switch (topo.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    // FIXME: BGPCEP-983: propagate all modifications
                    updateTopologyProvider(topo.getDataAfter());
                    break;
                case DELETE:
                    removeTopologyProvider(topo.getDataBefore());
                    break;
                default:
                    throw new IllegalStateException("Unhandled modification type " + topo.getModificationType());
            }
        }
    }

    @Holding("this")
    private void updateTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        LOG.info("Updating Topology {}", topologyId);
        final PCEPTopologyProviderBean previous = pcepTopologyServices.remove(topologyId);
        closeTopology(previous, topologyId);
        createTopologyProvider(topology);
    }

    @Holding("this")
    private void createTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        if (pcepTopologyServices.containsKey(topologyId)) {
            LOG.warn("Topology Provider {} already exist. New instance won't be created", topologyId);
            return;
        }
        LOG.info("Creating Topology {}", topologyId);
        LOG.trace("Topology {}.", topology);

        final PCEPTopologyProviderBean pcepTopologyProviderBean = new PCEPTopologyProviderBean(
            dataBroker, pcepDispatcher, rpcProviderRegistry, stateRegistry, pceServerProvider);
        pcepTopologyServices.put(topologyId, pcepTopologyProviderBean);

        pcepTopologyProviderBean.start(instructionSchedulerFactory, singletonService,
            new PCEPTopologyConfiguration(topology), bundleContext);
    }

    @Holding("this")
    private synchronized void removeTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        closeTopology(pcepTopologyServices.remove(topologyId), topologyId);
    }

    @Override
    public synchronized void close() {
        LOG.info("PCEP Topology Deployer closing");
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
        for (Map.Entry<TopologyId, PCEPTopologyProviderBean> entry : pcepTopologyServices.entrySet()) {
            closeTopology(entry.getValue(), entry.getKey());
        }
        pcepTopologyServices.clear();
        LOG.info("PCEP Topology Deployer closed");
    }

    private static void closeTopology(final PCEPTopologyProviderBean topology, final TopologyId topologyId) {
        if (topology == null) {
            return;
        }
        LOG.info("Removing Topology {}", topologyId);
        try {
            topology.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } catch (final InterruptedException | TimeoutException | ExecutionException e) {
            LOG.error("Topology {} instance failed to close service instance", topologyId, e);
        }
        topology.close();
    }

    private static boolean filterPcepTopologies(final @Nullable TopologyTypes topologyTypes) {
        if (topologyTypes == null) {
            return false;
        }
        final TopologyTypes1 aug = topologyTypes.augmentation(TopologyTypes1.class);
        return aug != null && aug.getTopologyPcep() != null;
    }
}