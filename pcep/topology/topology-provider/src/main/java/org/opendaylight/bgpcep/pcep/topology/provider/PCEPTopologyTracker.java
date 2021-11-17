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
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
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
public final class PCEPTopologyTracker implements ClusteredDataTreeChangeListener<TopologyPcep>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyTracker.class);

    // Services we are using
    private final InstructionSchedulerFactory instructionSchedulerFactory;
    private final ClusterSingletonServiceProvider singletonService;
    private final TopologySessionStatsRegistry stateRegistry;
    private final RpcProviderService rpcProviderRegistry;
    private final PceServerProvider pceServerProvider;
    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    // FIXME: BGPCEP-960: this should not be needed
    private final BundleContext bundleContext;

    // Internal state
    @GuardedBy("this")
    private final Map<TopologyKey, PCEPTopologyInstance> instances;
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
    public synchronized void close() {
        if (reg == null) {
            // Already closed, bail out
            return;
        }

        reg.close();
        reg = null;

        // FIXME: clean up all singletons
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<TopologyPcep>> changes) {
        if (reg == null) {
            return;
        }

        for (var change : changes) {
            final var root = change.getRootNode();
            switch (root.getModificationType()) {
                case WRITE:
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

    @Holding("this")
    private void createInstance(final @NonNull TopologyKey topology) {
        final var existing = instances.remove(topology);
        final PCEPTopologyInstance instance;
        if (existing == null) {
            LOG.info("Creating topology instance for {}", topology);
            instance = new PCEPTopologyInstance(this, topology);
        } else {
            LOG.info("Resurrecting topology instance for {}", topology);
            instance = existing.resurrect();
        }
        instances.put(topology, instance);
    }

    @Holding("this")
    private void destroyInstance(final @NonNull TopologyKey topology) {
        final var existing = instances.get(topology);
        if (existing != null) {
            existing.destroy();
        } else {
            LOG.warn("Attempted to destroy non-existent topology instance for {}", topology);
        }
    }

    private static @NonNull TopologyKey extractTopologyKey(final DataTreeModification<?> change) {
        final var path = change.getRootPath().getRootIdentifier();
        return verifyNotNull(path.firstKeyOf(Topology.class), "No topology key in %s", path);
    }
}
