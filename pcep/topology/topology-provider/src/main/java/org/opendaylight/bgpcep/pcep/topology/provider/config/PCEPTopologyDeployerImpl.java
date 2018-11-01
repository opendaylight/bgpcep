/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderUtil.filterPcepTopologies;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev171025.pcep.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.config.rev171025.PcepTopologyTypeConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private final BlueprintContainer container;
    private final InstanceIdentifier<NetworkTopology> networTopology;
    private final DataBroker dataBroker;
    private final InstructionSchedulerFactory instructionSchedulerFactory;
    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderBean> pcepTopologyServices = new HashMap<>();
    private ListenerRegistration<PCEPTopologyDeployerImpl> listenerRegistration;

    public PCEPTopologyDeployerImpl(final BlueprintContainer container,
            final DataBroker dataBroker, final InstructionSchedulerFactory instructionSchedulerFactory) {
        this.container = requireNonNull(container);
        this.dataBroker = requireNonNull(dataBroker);
        this.instructionSchedulerFactory = requireNonNull(instructionSchedulerFactory);
        this.networTopology = InstanceIdentifier.builder(NetworkTopology.class).build();
    }

    public synchronized void init() {
        LOG.info("PCEP Topology Deployer initialized");
        this.listenerRegistration = this.dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(CONFIGURATION, this.networTopology.child(Topology.class)), this);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        final List<DataObjectModification<Topology>> topoChanges = changes.stream()
                .map(DataTreeModification::getRootNode)
                .collect(Collectors.toList());
        topoChanges.stream().iterator().forEachRemaining(topo -> {
            switch (topo.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    updateTopologyProvider(topo.getDataAfter());
                    break;
                case DELETE:
                    removeTopologyProvider(topo.getDataBefore());
                    break;
                default:
            }
        });
    }

    private synchronized void updateTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        LOG.info("Updating Topology {}", topologyId);
        final PCEPTopologyProviderBean previous = this.pcepTopologyServices.remove(topology.getTopologyId());
        closeTopology(previous, topologyId);
        createTopologyProvider(topology);
    }

    private synchronized void createTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        if (this.pcepTopologyServices.containsKey(topology.getTopologyId())) {
            LOG.warn("Topology Provider {} already exist. New instance won't be created", topologyId);
            return;
        }
        LOG.info("Creating Topology {}", topologyId);
        LOG.trace("Topology {}.", topology);

        final SessionConfig config = topology.augmentation(PcepTopologyTypeConfig.class).getSessionConfig();
        final InstructionScheduler instructionScheduler = this.instructionSchedulerFactory
                .createInstructionScheduler(topologyId.getValue());

        final PCEPTopologyConfiguration dependencies = new PCEPTopologyConfiguration(config, topology);

        final PCEPTopologyProviderBean pcepTopologyProviderBean = (PCEPTopologyProviderBean) this.container
                .getComponentInstance(PCEPTopologyProviderBean.class.getSimpleName());
        this.pcepTopologyServices.put(topologyId, pcepTopologyProviderBean);
        pcepTopologyProviderBean.start(dependencies, instructionScheduler);
    }

    private synchronized void removeTopologyProvider(final Topology topology) {
        if (!filterPcepTopologies(topology.getTopologyTypes())) {
            return;
        }
        final TopologyId topologyId = topology.getTopologyId();
        closeTopology(this.pcepTopologyServices.remove(topologyId), topologyId);
    }

    @Override
    public synchronized void close() {
        LOG.info("PCEP Topology Deployer closing");
        if (this.listenerRegistration != null) {
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
        for (Map.Entry<TopologyId, PCEPTopologyProviderBean> entry:this.pcepTopologyServices.entrySet()) {
            closeTopology(entry.getValue(), entry.getKey());
        }
        this.pcepTopologyServices.clear();
        LOG.info("PCEP Topology Deployer closed");
    }


    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void closeTopology(final PCEPTopologyProviderBean topology, final TopologyId topologyId) {
        if (topology == null) {
            return;
        }
        LOG.info("Removing Topology {}", topologyId);
        try {
            topology.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            topology.close();
        } catch (final Exception e) {
            LOG.error("Topology {} instance failed to close service instance", topologyId, e);
        }
    }
}