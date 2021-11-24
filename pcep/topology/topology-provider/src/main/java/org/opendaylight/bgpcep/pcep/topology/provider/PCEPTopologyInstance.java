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

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tracks the configuration content of a particular topology instance and propagates updates towards its
 * associated {@link PCEPTopologyProvider}.
 */
final class PCEPTopologyInstance implements ClusteredDataTreeChangeListener<Topology> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyInstance.class);

    private final @NonNull TopologyKey topology;

    @GuardedBy("this")
    private PCEPTopologyProvider provider;
    @GuardedBy("this")
    private Registration reg;
    @GuardedBy("this")
    private ServiceRegistration<?> osgiReg;

    PCEPTopologyInstance(final TopologyKey topology, final PCEPTopologyProviderDependencies dependencies,
            final InstructionScheduler scheduler) {
        this.topology = requireNonNull(topology);

        final var instanceIdentifier = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topology);

        provider = new PCEPTopologyProvider(instanceIdentifier, dependencies, scheduler);

        reg = dependencies.getDataBroker().registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, instanceIdentifier), this);
        LOG.info("Topology instance for {} initialized", topologyId());
    }

    synchronized ListenableFuture<?> terminate() {
        verifyNotNull(reg, "Topology %s instance %s already terminating", topologyId(), this);
        reg.close();
        reg = null;

        osgiReg.unregister();
        osgiReg = null;

        final var ret = provider.stop();
        provider = null;
        return ret;
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        if (reg == null) {
            // We have been shut down, do not process any more updates
            return;
        }

        // We are only interested in the after-image
        final var content = Iterables.getLast(changes).getRootNode().getDataAfter();
        if (content != null) {
            LOG.trace("Updating topology {} configuration to {}", topologyId(), content);
            provider.updateConfiguration(PCEPTopologyConfiguration.of(content));
        } else {
            LOG.info("Topology {} configuration disappeared, ignoring update in anticipation of shutdown",
                topologyId());
        }
    }

    private String topologyId() {
        return TopologyUtils.friendlyId(topology);
    }
}
