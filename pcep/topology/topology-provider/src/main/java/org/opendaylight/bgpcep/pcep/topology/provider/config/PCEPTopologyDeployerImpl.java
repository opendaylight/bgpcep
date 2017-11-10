/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.pcep.topology.config.rev171025.PcepTopologyTypeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev171025.pcep.config.SessionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements PCEPTopologyDeployer,
        ClusteredDataTreeChangeListener<Topology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);

    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderBean> pcepTopologyServices = new HashMap<>();
    private final BlueprintContainer container;
    private final InstanceIdentifier<NetworkTopology> networTopology;
    private final ListenerRegistration<PCEPTopologyDeployerImpl> listenerRegistration;
    private final DataBroker dataBroker;
    private final InstructionSchedulerFactory instructionSchedulerFactory;

    public PCEPTopologyDeployerImpl(final BlueprintContainer container,
            final DataBroker dataBroker, final InstructionSchedulerFactory instructionSchedulerFactory) {
        this.container = requireNonNull(container);
        this.dataBroker = requireNonNull(dataBroker);
        this.instructionSchedulerFactory = requireNonNull(instructionSchedulerFactory);
        this.networTopology = InstanceIdentifier.builder(NetworkTopology.class).build();
        this.listenerRegistration = this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(CONFIGURATION, this.networTopology.child(Topology.class)), this);
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Topology>> changes) {
        final List<DataObjectModification<Topology>> topoChanges = changes.stream()
                .map(DataTreeModification::getRootNode)
                .collect(Collectors.toList());
        topoChanges.stream()
                .filter(rootNode -> rootNode.getModificationType().equals(ModificationType.DELETE))
                .map(DataObjectModification::getDataBefore)
                .filter(topo -> filterPcepTopologies(topo.getTopologyTypes()))
                .forEach(topology -> removeTopologyProvider(topology.getTopologyId()));

        topoChanges.stream()
                .filter(rootNode -> rootNode.getModificationType().equals(ModificationType.SUBTREE_MODIFIED))
                .map(DataObjectModification::getDataAfter)
                .filter(topo -> filterPcepTopologies(topo.getTopologyTypes()))
                .forEach(this::updateTopologyProvider);

        topoChanges.stream()
                .filter(rootNode -> rootNode.getModificationType().equals(ModificationType.WRITE))
                .map(DataObjectModification::getDataAfter)
                .filter(topo -> filterPcepTopologies(topo.getTopologyTypes()))
                .forEach(this::createTopologyProvider);
    }

    private boolean filterPcepTopologies(final TopologyTypes topologyTypes) {
        if (topologyTypes == null) {
            return false;
        }
        final TopologyTypes1 aug = topologyTypes.getAugmentation(TopologyTypes1.class);

        return aug != null && aug.getTopologyPcep() != null;
    }

    private void updateTopologyProvider(final Topology topology) {
        final TopologyId topologyId = topology.getTopologyId();
        final PCEPTopologyProviderBean previous = this.pcepTopologyServices.remove(topology.getTopologyId());
        if (previous != null) {
            try {
                previous.closeServiceInstance().get();
            } catch (final Exception e) {
                LOG.error("Topology {} instance failed to close service instance", topologyId, e);
            }
            previous.close();
        }
        createTopologyProvider(topology);
    }

    private synchronized void createTopologyProvider(final Topology topology) {
        final TopologyId topologyId = topology.getTopologyId();
        if (this.pcepTopologyServices.containsKey(topology.getTopologyId())) {
            LOG.warn("Topology Provider {} already exist. New instance won't be created", topologyId);
            return;
        }

        final PcepTopologyTypeConfig pcepTopo = topology.getAugmentation(PcepTopologyTypeConfig.class);

        final SessionConfig config = pcepTopo.getSessionConfig();

        final InstructionScheduler instructionScheduler = this.instructionSchedulerFactory
                .createInstructionScheduler(topologyId.getValue());

        final InetSocketAddress inetAddress = PCEPTopologyProviderUtil
                .getInetSocketAddress(config.getListenAddress(), config.getListenPort());

        final KeyMapping keys = PCEPTopologyProviderUtil.contructKeys(topology);
        final PCEPTopologyConfigDependencies dependencies = new PCEPTopologyConfigDependencies(
                inetAddress, keys, instructionScheduler, topology.getTopologyId(),
                config.getRpcTimeout());

        final PCEPTopologyProviderBean pcepTopologyProviderBean = (PCEPTopologyProviderBean) this.container
                .getComponentInstance(PCEPTopologyProviderBean.class.getSimpleName());
        this.pcepTopologyServices.put(topologyId, pcepTopologyProviderBean);
        pcepTopologyProviderBean.start(dependencies);
    }

    private synchronized void removeTopologyProvider(final TopologyId topologyId) {
        final PCEPTopologyProviderBean topology = this.pcepTopologyServices.remove(topologyId);
        if (topology != null) {
            try {
                topology.closeServiceInstance().get();
            } catch (final Exception e) {
                LOG.error("Topology {} instance failed to close service instance", topologyId, e);
            }
            topology.close();
        }
    }

    @Override
    public synchronized ListenableFuture<Void> writeConfiguration(final Topology pcepTopology) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        final TopologyId topologyId = pcepTopology.getTopologyId();
        final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIID = this.networTopology
                .child(Topology.class, new TopologyKey(topologyId));
        wTx.put(CONFIGURATION, topologyIID, pcepTopology, true);
        final ListenableFuture<Void> future = wTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("PCEP Topology configuration {} stored successfully.",
                        pcepTopology.getTopologyId().getValue());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to store PCEP Topology configuration {}.",
                        pcepTopology.getTopologyId().getValue(), t);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public synchronized ListenableFuture<Void> removeConfiguration(final TopologyId topologyId) {
        final WriteTransaction wTx = this.dataBroker.newWriteOnlyTransaction();
        final TopologyKey topoKey = new TopologyKey(topologyId);
        final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIID = this.networTopology
                .child(Topology.class, topoKey);
        wTx.delete(CONFIGURATION, topologyIID);
        final ListenableFuture<Void> future = wTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} removed successfully.", topologyId);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to remove Instruction Instance {}.", topologyId, t);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }


    @Override
    public synchronized void close() throws Exception {
        this.listenerRegistration.close();
        this.pcepTopologyServices.values().forEach(PCEPTopologyProviderBean::close);
    }
}