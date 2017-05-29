/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.base.Optional;
import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * The PCEPTopologyDeployer service is managing PcepTopologyProvider
 */
public interface PCEPTopologyDeployer {
    /**
     * Creates and register topology provider instance
     *  @param topologyId topology ID
     * @param inetSocketAddress inetSocketAddress
     * @param rpcTimeout rpc Timeout
     * @param keys List of clients password configuration
     * @param scheduler  Instruction Scheduler
     */
    @Deprecated
    default void createTopologyProvider(@Nonnull TopologyId topologyId, @Nonnull InetSocketAddress inetSocketAddress,
        short rpcTimeout, @Nullable Optional<KeyMapping> keys, @Nonnull InstructionScheduler scheduler,
        Optional<PCEPTopologyProviderRuntimeRegistrator> runtime) {
        if(keys.isPresent()) {
            createTopologyProvider(topologyId, inetSocketAddress, rpcTimeout, keys.get(), scheduler, runtime);
        }
        createTopologyProvider(topologyId, inetSocketAddress, rpcTimeout, KeyMapping.getKeyMapping(),
            scheduler, runtime);
    }

    /**
     * Creates and register topology provider instance
     *  @param topologyId topology ID
     * @param inetSocketAddress inetSocketAddress
     * @param rpcTimeout rpc Timeout
     * @param client List of clients password configuration
     * @param scheduler  Instruction Scheduler
     */
    void createTopologyProvider(@Nonnull TopologyId topologyId, @Nonnull InetSocketAddress inetSocketAddress,
        short rpcTimeout, @Nonnull KeyMapping client, @Nonnull InstructionScheduler scheduler,
        Optional<PCEPTopologyProviderRuntimeRegistrator> runtime);

    /**
     * Closes and unregister topology provider instance
     *
     * @param topologyID topology ID
     */
    void removeTopologyProvider(@Nonnull TopologyId topologyID);
}
