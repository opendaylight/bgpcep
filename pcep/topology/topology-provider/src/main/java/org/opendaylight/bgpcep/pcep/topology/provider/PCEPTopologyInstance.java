/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.binding.api.DataListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tracks the configuration content of a particular topology instance and propagates updates towards its
 * associated {@link PCEPTopologyProvider}.
 */
final class PCEPTopologyInstance implements DataListener<Topology> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyInstance.class);

    private final AtomicBoolean closed = new AtomicBoolean();
    private final @NonNull TopologyKey topology;

    @GuardedBy("this")
    private PCEPTopologyProvider provider;
    // accessed once via terminate(), single-threadedness is guarded by 'closed' false->true CAS
    private Registration reg;
    // accessed from dataChangedTo() only
    private boolean initialReceived;

    @NonNullByDefault
    PCEPTopologyInstance(final TopologyKey topology, final PCEPTopologyProviderDependencies dependencies,
            final InstructionScheduler scheduler) {
        this.topology = requireNonNull(topology);

        final var instanceIdentifier = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, topology)
            .build();

        // FIXME: inject provider from the caller for clarity of dependencies
        provider = new PCEPTopologyProvider(instanceIdentifier, dependencies, scheduler);
        reg = dependencies.getDataBroker()
            .registerDataListener(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, this);
        LOG.info("Topology instance for {} initialized", topologyId());
    }

    ListenableFuture<?> terminate() {
        // assert transition outside of lock
        if (!closed.compareAndSet(false, true)) {
            throw new VerifyException("Topology %s instance %s already terminating".formatted(topologyId(), this));
        }
        // stop callbacks
        reg.close();
        reg = null;

        return clearProvider().stop();
    }

    // let running callbacks finish and prevent pending callbacks from observing 'closed'
    private synchronized PCEPTopologyProvider clearProvider() {
        final var ret = provider;
        provider = null;
        return ret;
    }

    @Override
    public synchronized void dataChangedTo(final Topology data) {
        if (closed.getAcquire()) {
            // we have started to shut down, do not process any more updates
            LOG.trace("Topology {} instance is closed, ignoring update", topologyId());
            return;
        }

        if (!initialReceived) {
            initialReceived = true;
            LOG.debug("Topology {} instance received initial notification", topologyId());
        }

        if (data != null) {
            LOG.trace("Updating topology {} configuration to {}", topologyId(), data);
            provider.updateConfiguration(PCEPTopologyConfiguration.of(data));
        } else {
            LOG.info("Topology {} configuration disappeared, ignoring update in anticipation of shutdown",
                topologyId());
        }
    }

    private String topologyId() {
        return TopologyUtils.friendlyId(topology);
    }
}
