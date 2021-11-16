/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
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
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private final InstructionSchedulerFactory instructionSchedulerFactory;
    private final TopologySessionListenerFactory sessionListenerFactory;
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
            final TopologySessionListenerFactory sessionListenerFactory,
            final InstructionSchedulerFactory instructionSchedulerFactory,
            final TopologySessionStatsRegistry stateRegistry, final PceServerProvider pceServerProvider,
            // FIXME: we should not be needing this OSGi dependency
            final BundleContext bundleContext) {
        this.dataBroker = requireNonNull(dataBroker);
        this.singletonService = requireNonNull(singletonService);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.pcepDispatcher = requireNonNull(pcepDispatcher);
        this.sessionListenerFactory = requireNonNull(sessionListenerFactory);
        this.instructionSchedulerFactory = requireNonNull(instructionSchedulerFactory);
        this.stateRegistry = requireNonNull(stateRegistry);
        this.pceServerProvider = requireNonNull(pceServerProvider);
        this.bundleContext = requireNonNull(bundleContext);
    }

    public synchronized void init() {
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
                    onTopologyModified(requireBeforeImage(topo), requireAfterImage(topo));
                    break;
                case WRITE:
                    onTopologyWritten(topo);
                    break;
                case DELETE:
                    onTopologyDeleted(topo);
                    break;
                default:
                    throw new IllegalStateException("Unhandled modification type " + topo.getModificationType());
            }
        }
    }

    @Holding("this")
    private void onTopologyDeleted(final DataObjectModification<Topology> modification) {
        // This is rather straightforward: just check if the deleted topology is something we used to care about and if
        // so shutdown the associated provided
        final var before = requireBeforeImage(modification);
        if (isPcepTopology(before)) {
            destroyProvider(before);
        }
    }

    @Holding("this")
    private void onTopologyModified(final @NonNull Topology before, final @NonNull Topology after) {
        // This is a bit tricky. Usually this is just an update to the configuration, but it might also be that the
        // topology has become interesting, or conversely uninteresting.
        if (isPcepTopology(before)) {
            if (isPcepTopology(after)) {
                updateProvider(after);
            } else {
                destroyProvider(before);
            }
        } else if (isPcepTopology(after)) {
            createProvider(after);
        }
    }

    @Holding("this")
    private void onTopologyWritten(final DataObjectModification<Topology> modification) {
        // This can either be a newly-introduced topology or it can be an overwrite.
        final var before = modification.getDataBefore();
        final var after = requireAfterImage(modification);
        if (before != null) {
            onTopologyModified(before, after);
        } else if (isPcepTopology(after)) {
            createProvider(after);
        }
    }

    @Holding("this")
    private void createProvider(final @NonNull Topology topology) {
        final var topologyId = topology.requireTopologyId();
        final var existing = pcepTopologyServices.get(topologyId);
        if (existing != null) {
            LOG.warn("Topology Provider {} already exists, ignoring an attempt to re-create", topologyId);
            return;
        }

        LOG.info("Creating Topology Provider {}", topologyId);
        LOG.trace("Topology {}.", topology);
        final InstructionScheduler instructionScheduler = instructionSchedulerFactory
                .createInstructionScheduler(topologyId.getValue());

        final PCEPTopologyProviderBean pcepTopologyProviderBean = new PCEPTopologyProviderBean(singletonService,
            bundleContext, dataBroker, pcepDispatcher, rpcProviderRegistry, sessionListenerFactory, stateRegistry,
            pceServerProvider);
        pcepTopologyServices.put(topologyId, pcepTopologyProviderBean);

        pcepTopologyProviderBean.start(new PCEPTopologyConfiguration(topology), instructionScheduler);
    }

    @Holding("this")
    private void destroyProvider(final @NonNull Topology topology) {
        final var topologyId = topology.requireTopologyId();
        final var existing = pcepTopologyServices.remove(topologyId);
        if (existing != null) {
            closeTopology(existing, topologyId);
        } else {
            LOG.warn("Topology Provider {} not found, ignoring mismatch", topologyId);
        }
    }

    @Holding("this")
    private void updateProvider(final @NonNull Topology topology) {
        final var topologyId = topology.requireTopologyId();
        final var existing = pcepTopologyServices.get(topologyId);
        if (existing == null) {
            LOG.warn("Topology Provider {} not found, cannot update its configuration", topologyId);
            return;
        }

        // FIXME: finish this

//      final TopologyId topologyId = topology.requireTopologyId();
//      LOG.info("Updating Topology {}", topologyId);
//      closeTopology(previous, topologyId);
//      createTopologyProvider(topology);
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

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void closeTopology(final PCEPTopologyProviderBean topology, final TopologyId topologyId) {
        LOG.info("Removing Topology {}", topologyId);
        try {
            // FIXME: this should not be synchronous
            topology.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            topology.close();
        } catch (final Exception e) {
            LOG.error("Topology {} instance failed to close service instance", topologyId, e);
        }
    }

    private static boolean isPcepTopology(final Topology topology) {
        final var types = topology.getTopologyTypes();
        if (types == null) {
            return false;
        }

        final var augment = types.augmentation(TopologyTypes1.class);
        return augment != null && augment.getTopologyPcep() != null;
    }

    private static @NonNull Topology requireAfterImage(final DataObjectModification<Topology> modification) {
        return verifyNotNull(modification.getDataAfter(), "Missing after-image in %s", modification);
    }

    private static @NonNull Topology requireBeforeImage(final DataObjectModification<Topology> modification) {
        return verifyNotNull(modification.getDataBefore(), "Missing before-image in %s", modification);
    }
}