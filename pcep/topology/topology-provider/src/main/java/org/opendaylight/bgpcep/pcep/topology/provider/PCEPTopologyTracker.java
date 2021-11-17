/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependencies;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.topology.pcep.type.TopologyPcep;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary entrypoint into this component. Once an instance of this class is instantiated, it will subscribe to
 * changes to the configuration datastore. There it filters only topologies which have {@link TopologyPcep} type and for
 * each one of those instantiates a cluster-wide singleton to handle lifecycle of services attached to that topology.
 */
public final class PCEPTopologyTracker
        implements PCEPTopologyProviderDependencies, ClusteredDataTreeChangeListener<TopologyPcep>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyTracker.class);

    // Services we are using
    final @NonNull InstructionSchedulerFactory instructionSchedulerFactory;
    final @NonNull ClusterSingletonServiceProvider singletonService;
    private final @NonNull TopologySessionStatsRegistry stateRegistry;
    private final @NonNull RpcProviderService rpcProviderRegistry;
    private final @NonNull PceServerProvider pceServerProvider;
    private final @NonNull PCEPDispatcher pcepDispatcher;
    private final @NonNull DataBroker dataBroker;
    // FIXME: BGPCEP-960: this should not be needed
    private final @NonNull BundleContext bundleContext;

    // We are reusing our monitor as the universal lock. We have to account for three distinct threads competing for
    // our state:
    //   1) the typical DTCL callback thread invoking onDataTreeChanged()
    //   2) instance cleanup thread invoking finishDestroy()
    //   3) framework shutdown thread invoking close()
    //
    // We need to track not only instances which are deemed alive by the class, but also all instances for which cleanup
    // has not finished yet, so close() can properly wait for cleanup to finish.
    //
    // Since close() will terminate the DTCL subscription, the synchronization between 1) and 3) is rather trivial.
    //
    // The interaction between DTCL and cleanup is tricky. DTCL can report rapid create/destroy/create events and
    // cleanup is asynchronous and when the dust settles we need to end up in the corrected overall state (created or
    // destroyed).
    //
    // In order to achieve that without risking deadlocks, instances are tracked using a concurrent map and each
    // 'create' edge allocates a new PCEPTopologyInstance object.
    private final ConcurrentMap<TopologyKey, PCEPTopologySingleton> instances = new ConcurrentHashMap<>();
    @GuardedBy("this")
    private Registration reg;

    public PCEPTopologyTracker(final DataBroker dataBroker, final ClusterSingletonServiceProvider singletonService,
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

        reg = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(TopologyTypes.class)
                .augmentation(TopologyTypes1.class).child(TopologyPcep.class).build()), this);
        LOG.info("PCEP Topology tracker initialized");
    }

    @Override
    public PCEPDispatcher getPCEPDispatcher() {
        return pcepDispatcher;
    }

    @Override
    public RpcProviderService getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public TopologySessionStatsRegistry getStateRegistry() {
        return stateRegistry;
    }

    @Override
    public PceServerProvider getPceServerProvider() {
        return pceServerProvider;
    }

    @Override
    public synchronized void close() {
        if (reg == null) {
            // Already closed, bail out
            return;
        }

        LOG.info("PCEP Topology tracker shutting down");
        reg.close();
        reg = null;

        // First pass: destroy all tracked instances
        instances.values().forEach(PCEPTopologySingleton::destroy);
        // Second pass: wait for cleanup
        instances.values().forEach(PCEPTopologySingleton::awaitCleanup);
        LOG.info("PCEP Topology tracker shut down");
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<TopologyPcep>> changes) {
        if (reg == null) {
            // Registration has been terminated, do not process any changes
            return;
        }

        for (var change : changes) {
            final var root = change.getRootNode();
            switch (root.getModificationType()) {
                case WRITE:
                    // We only care if the topology has been newly introduced, not when its details have changed
                    if (root.getDataBefore() == null) {
                        createInstance(extractTopologyKey(change));
                    }
                    break;
                case DELETE:
                    destroyInstance(extractTopologyKey(change));
                    break;
                default:
                    // No-op
            }
        }
    }

    private void createInstance(final @NonNull TopologyKey topology) {
        final var existing = instances.remove(topology);
        final PCEPTopologySingleton instance;
        if (existing == null) {
            LOG.info("Creating topology instance for {}", topology);
            instance = new PCEPTopologySingleton(this, topology);
        } else {
            LOG.info("Resurrecting topology instance for {}", topology);
            instance = existing.resurrect();
        }
        instances.put(topology, instance);
    }

    private void destroyInstance(final @NonNull TopologyKey topology) {
        final var existing = instances.get(topology);
        if (existing != null) {
            LOG.info("Destroying topology instance for {}", topology);
            existing.destroy();
        } else {
            LOG.warn("Attempted to destroy non-existent topology instance for {}", topology);
        }
    }

    void finishDestroy(final TopologyKey topology, final PCEPTopologySingleton instance) {
        if (instances.remove(topology, instance)) {
            LOG.info("Destroyed topology instance of {}", topology);
        }
    }

    private static @NonNull TopologyKey extractTopologyKey(final DataTreeModification<?> change) {
        final var path = change.getRootPath().getRootIdentifier();
        return verifyNotNull(path.firstKeyOf(Topology.class), "No topology key in %s", path);
    }
}
