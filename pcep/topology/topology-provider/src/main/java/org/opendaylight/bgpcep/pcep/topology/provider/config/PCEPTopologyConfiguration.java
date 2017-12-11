/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTopologyConfiguration {
    private final InetSocketAddress address;
    private final KeyMapping keys;
    private final InstructionScheduler scheduler;
    private final TopologyId topologyId;
    private final short rpcTimeout;
    private final InstanceIdentifier<Topology> topology;

    public PCEPTopologyConfiguration(
            @Nonnull final InetSocketAddress address,
            @Nonnull final KeyMapping keys,
            @Nonnull final InstructionScheduler scheduler,
            @Nonnull final TopologyId topologyId,
            final short rpcTimeout) {
        this.address = checkNotNull(address);
        this.keys = checkNotNull(keys);
        this.scheduler = checkNotNull(scheduler);
        this.topologyId = checkNotNull(topologyId);
        this.rpcTimeout = rpcTimeout;
        this.topology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(this.topologyId)).build();
    }

    @Nonnull
    public TopologyId getTopologyId() {
        return this.topologyId;
    }

    @Nonnull
    public InstanceIdentifier<Topology> getTopology() {
        return this.topology;
    }

    @Nonnull
    public InstructionScheduler getSchedulerDependency() {
        return this.scheduler;
    }

    public short getRpcTimeout() {
        return this.rpcTimeout;
    }

    @Nonnull
    public InetSocketAddress getAddress() {
        return this.address;
    }

    @Nonnull
    public KeyMapping getKeys() {
        return this.keys;
    }
}
