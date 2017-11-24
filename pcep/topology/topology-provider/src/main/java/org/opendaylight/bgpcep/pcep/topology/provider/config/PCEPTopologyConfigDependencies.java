/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public final class PCEPTopologyConfigDependencies {
    private final InetSocketAddress address;
    private final KeyMapping keys;
    private final InstructionScheduler scheduler;
    private final TopologyId topologyId;
    private final short rpcTimeout;

    public PCEPTopologyConfigDependencies(final InetSocketAddress address, final KeyMapping keys,
            final InstructionScheduler scheduler, final TopologyId topologyId, final short rpcTimeout) {
        this.address = checkNotNull(address);
        this.keys = checkNotNull(keys);
        this.scheduler = checkNotNull(scheduler);
        this.topologyId = checkNotNull(topologyId);
        this.rpcTimeout = rpcTimeout;
    }

    public TopologyId getTopologyId() {
        return this.topologyId;
    }

    public InstructionScheduler getSchedulerDependency() {
        return this.scheduler;
    }

    public short getRpcTimeout() {
        return this.rpcTimeout;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public KeyMapping getKeys() {
        return this.keys;
    }
}
